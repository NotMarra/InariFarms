package dev.notmarra.inarifarms.listeners

import dev.notmarra.inarifarms.data.BlockDataManager
import dev.notmarra.inarifarms.items.ItemManager
import dev.notmarra.inarifarms.stations.StationRegistry
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.persistence.PersistentDataType

class StationBlockListener(
    private val blockDataManager: BlockDataManager,
    private val itemManager: ItemManager,
    private val stationRegistry: StationRegistry
    ) : Listener {
    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        val meta = item.itemMeta ?: return

        val stationId = meta.persistentDataContainer
            .get(itemManager.customItemKey, PersistentDataType.STRING)
            ?: return

        val station = stationRegistry.getStation(stationId) ?: return

        val block = event.blockPlaced
        val ownerUuid = event.player.uniqueId.toString()

        blockDataManager.setStation(block, station.fullId, level = 1, ownerUuid = ownerUuid)

        event.player.sendMessage("Stanice ${station.displayName} postavena!")
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block

        if (!blockDataManager.isStation(block)) return

        event.isDropItems = false

        val stationId = blockDataManager.getStationType(block) ?: return
        val station = stationRegistry.getStation(stationId) ?: return

        block.world.dropItemNaturally(block.location, itemManager.createStationItem(station))

        blockDataManager.removeStation(block)
        event.player.sendMessage("Stanice ${station.displayName} zničena!")
    }
}