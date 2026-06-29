package dev.notmarra.inarifarms.data

import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class BlockDataManager(private val plugin: Plugin) {
    private val stationTypeKey = NamespacedKey(plugin, "station_type")
    private val stationLevelKey = NamespacedKey(plugin, "station_level")
    private val stationOwnerKey = NamespacedKey(plugin, "station_owner")

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

    fun setStation(block: Block, stationTypeId: String, level: Int, ownerUuid: String) {
        val chunk = block.chunk
        val pdc = chunk.persistentDataContainer

        pdc.set(stationTypeKeyForBlock(block), PersistentDataType.STRING, stationTypeId)
        pdc.set(stationLevelKeyForBlock(block), PersistentDataType.INTEGER, level)
        pdc.set(stationOwnerKeyForBlock(block), PersistentDataType.STRING, ownerUuid)
    }

    fun getStationType(block: Block): String? {
        val pdc = block.chunk.persistentDataContainer
        return pdc.get(stationTypeKeyForBlock(block), PersistentDataType.STRING)
    }

    fun getStationLevel(block: Block): Int? {
        val pdc = block.chunk.persistentDataContainer
        return pdc.get(stationLevelKeyForBlock(block), PersistentDataType.INTEGER)
    }

    fun isStation(block: Block): Boolean {
        return getStationType(block) != null
    }

    fun removeStation(block: Block) {
        val pdc = block.chunk.persistentDataContainer
        pdc.remove(stationTypeKeyForBlock(block))
        pdc.remove(stationLevelKeyForBlock(block))
        pdc.remove(stationOwnerKeyForBlock(block))
    }
}