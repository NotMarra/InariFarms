package dev.notmarra.inarifarms.listeners

import dev.notmarra.inarifarms.gui.StationGui
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

class StationGuiListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.getHolder(false) as? StationGui ?: return

        val slot = event.rawSlot
        val topSize = event.inventory.size // 27

        when {
            // Klik v dolním inventáři hráče — blokujeme shift+click nahoru
            slot >= topSize -> {
                if (event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.isCancelled = true
                }
            }

            // Seed slot — jen inari semínko
            slot in holder.seedSlotIndices -> {
                when (event.action) {
                    InventoryAction.PLACE_ALL,
                    InventoryAction.PLACE_ONE,
                    InventoryAction.PLACE_SOME,
                    InventoryAction.SWAP_WITH_CURSOR -> {
                        val cursor = event.cursor
                        if (cursor == null || cursor.type == Material.AIR || !holder.isSeedItem(cursor)) {
                            event.isCancelled = true
                        }
                    }
                    // Vytahování povolíme
                    InventoryAction.PICKUP_ALL,
                    InventoryAction.PICKUP_HALF,
                    InventoryAction.PICKUP_ONE,
                    InventoryAction.PICKUP_SOME,
                    InventoryAction.MOVE_TO_OTHER_INVENTORY -> { /* ok */ }

                    else -> event.isCancelled = true
                }
            }

            // Storage slot — volný přístup
            slot in holder.storageSlotIndices -> { /* ok */ }

            // Filler, info slot a vše ostatní — cancel
            else -> event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val holder = event.inventory.getHolder(false) as? StationGui ?: return
        val allowedSlots = holder.seedSlotIndices + holder.storageSlotIndices

        // Pokud drag zasahuje do zakázaného slotu v horním inventáři — cancel
        if (event.rawSlots.any { it < event.inventory.size && it !in allowedSlots }) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.inventory.getHolder(false) as? StationGui ?: return
        holder.saveToBlock()
    }
}