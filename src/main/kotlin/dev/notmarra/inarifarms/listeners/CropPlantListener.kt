package dev.notmarra.inarifarms.listeners

import dev.notmarra.inarifarms.crops.CropRegistry
import dev.notmarra.inarifarms.data.CropState
import dev.notmarra.inarifarms.items.ItemManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType

class CropPlantListener(
    private val itemManager: ItemManager,
    private val cropRegistry: CropRegistry
): Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val item = event.item ?: return
        val meta = item.itemMeta ?: return
        val customId = meta.persistentDataContainer.get(itemManager.customItemKey, PersistentDataType.STRING) ?: return
        val crop = cropRegistry.getCrop(customId) ?: return

        if (crop in cropRegistry.getAllCrops()) {
            event.isCancelled = true
        }
    }
}