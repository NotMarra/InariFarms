package dev.notmarra.inarifarms.listeners

import dev.notmarra.inarifarms.crops.CropRegistry
import dev.notmarra.inarifarms.data.CropDataManager
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
    private val dataManager: CropDataManager,
    private val itemManager: ItemManager,
    private val cropRegistry: CropRegistry
): Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return
        if (clickedBlock.type != Material.FARMLAND) return

        val item = event.item ?: return
        val meta = item.itemMeta ?: return

        val customId = meta.persistentDataContainer.get(itemManager.customItemKey, PersistentDataType.STRING) ?: return

        val crop = cropRegistry.getCrop(customId) ?: return

        val cropBlock = clickedBlock.getRelative(BlockFace.UP)

        if (cropBlock.type == Material.AIR) {
            event.isCancelled = true
            item.amount -= 1

            cropBlock.type = Material.CARROTS

            val newState = CropState(crop.fullId, 1, 100)
            dataManager.saveCropData(cropBlock, newState)
        }
    }
}