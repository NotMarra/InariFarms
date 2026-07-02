package dev.notmarra.inarifarms.data

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class BlockDataManager(private val plugin: Plugin) {

    private fun blockKey(block: Block): String {
        fun Int.toSafe() = if (this < 0) "n${-this}" else "$this"
        return "${block.world.name.lowercase()}_${block.x.toSafe()}_${block.y.toSafe()}_${block.z.toSafe()}"
    }

    private fun stationTypeKeyForBlock(block: Block) =
        NamespacedKey(plugin, "station_type_${blockKey(block)}")

    private fun stationLevelKeyForBlock(block: Block) =
        NamespacedKey(plugin, "station_level_${blockKey(block)}")

    private fun stationOwnerKeyForBlock(block: Block) =
        NamespacedKey(plugin, "station_owner_${blockKey(block)}")

    private fun seedSlotKeyForBlock(block: Block) =
        NamespacedKey(plugin, "seed_${blockKey(block)}")

    private fun storageSlotKeyForBlock(block: Block, slot: Int) =
        NamespacedKey(plugin, "storage_${slot}_${blockKey(block)}")


    private fun cropIdKeyForBlock(block: Block) =
        NamespacedKey(plugin, "crop_id_${blockKey(block)}")

    private fun cropStageKeyForBlock(block: Block) =
        NamespacedKey(plugin, "crop_stage_${blockKey(block)}")

    private fun cropMoistureKeyForBlock(block: Block) =
        NamespacedKey(plugin, "crop_moisture_${blockKey(block)}")

    private fun cropNextGrowthKeyForBlock(block: Block) =
        NamespacedKey(plugin, "crop_next_growth_${blockKey(block)}")

    private fun stationWaterKeyForBlock(block: Block) =
        NamespacedKey(plugin, "station_water_${blockKey(block)}")


    fun setStation(block: Block, stationTypeId: String, level: Int, ownerUuid: String) {
        val pdc = block.chunk.persistentDataContainer
        pdc.set(stationTypeKeyForBlock(block), PersistentDataType.STRING, stationTypeId)
        pdc.set(stationLevelKeyForBlock(block), PersistentDataType.INTEGER, level)
        pdc.set(stationOwnerKeyForBlock(block), PersistentDataType.STRING, ownerUuid)
    }

    fun getStationType(block: Block): String? {
        val pdc = block.chunk.persistentDataContainer
        return pdc.get(stationTypeKeyForBlock(block), PersistentDataType.STRING)
    }

    fun getStationLevel(block: Block): Int {
        val pdc = block.chunk.persistentDataContainer
        return pdc.get(stationLevelKeyForBlock(block), PersistentDataType.INTEGER) ?: 1
    }

    fun isStation(block: Block): Boolean = getStationType(block) != null

    fun removeStation(block: Block) {
        val pdc = block.chunk.persistentDataContainer
        pdc.remove(stationTypeKeyForBlock(block))
        pdc.remove(stationLevelKeyForBlock(block))
        pdc.remove(stationOwnerKeyForBlock(block))
        removeCropState(block)
        setSeedSlotItem(block, null)
    }


    fun setSeedSlotItem(block: Block,  item: ItemStack?) {
        val pdc = block.chunk.persistentDataContainer
        val key = seedSlotKeyForBlock(block)
        if (item == null || item.type == Material.AIR) {
            pdc.remove(key)
        } else {
            pdc.set(key, PersistentDataType.BYTE_ARRAY, item.serializeAsBytes())
        }
    }

    fun getSeedSlotItem(block: Block): ItemStack? {
        val pdc = block.chunk.persistentDataContainer
        val bytes = pdc.get(seedSlotKeyForBlock(block), PersistentDataType.BYTE_ARRAY) ?: return null
        return ItemStack.deserializeBytes(bytes)
    }


    fun setStorageSlotItem(block: Block, slot: Int, item: ItemStack?) {
        val pdc = block.chunk.persistentDataContainer
        val key = storageSlotKeyForBlock(block, slot)
        if (item == null || item.type == Material.AIR) {
            pdc.remove(key)
        } else {
            pdc.set(key, PersistentDataType.BYTE_ARRAY, item.serializeAsBytes())
        }
    }

    fun getStorageSlotItem(block: Block, slot: Int): ItemStack? {
        val pdc = block.chunk.persistentDataContainer
        val bytes = pdc.get(storageSlotKeyForBlock(block, slot), PersistentDataType.BYTE_ARRAY) ?: return null
        return ItemStack.deserializeBytes(bytes)
    }


    fun setCropState(block: Block, state: CropState?) {
        val pdc = block.chunk.persistentDataContainer
        if (state == null) {
            removeCropState(block)
            return
        }
        pdc.set(cropIdKeyForBlock(block), PersistentDataType.STRING, state.cropTypeId)
        pdc.set(cropStageKeyForBlock(block), PersistentDataType.INTEGER, state.currentStage)
        pdc.set(cropNextGrowthKeyForBlock(block), PersistentDataType.LONG, state.nextGrowthTime)
    }

    fun getCropState(block: Block): CropState? {
        val pdc = block.chunk.persistentDataContainer
        val id = pdc.get(cropIdKeyForBlock(block), PersistentDataType.STRING) ?: return null
        return CropState(
            cropTypeId = id,
            currentStage = pdc.get(cropStageKeyForBlock(block), PersistentDataType.INTEGER) ?: 0,
            nextGrowthTime = pdc.get(cropNextGrowthKeyForBlock(block), PersistentDataType.LONG) ?: 0L
        )
    }

    fun removeCropState(block: Block) {
        val pdc = block.chunk.persistentDataContainer
        pdc.remove(cropIdKeyForBlock(block))
        pdc.remove(cropStageKeyForBlock(block))
        pdc.remove(cropMoistureKeyForBlock(block))
        pdc.remove(cropNextGrowthKeyForBlock(block))
    }

    fun setStationWater(block: Block, water: Int) {
        block.chunk.persistentDataContainer.set(stationWaterKeyForBlock(block), PersistentDataType.INTEGER, water)
    }

    fun getStationWater(block: Block): Int {
        return block.chunk.persistentDataContainer.get(stationWaterKeyForBlock(block), PersistentDataType.INTEGER) ?: 0
    }
}