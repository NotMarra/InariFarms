package dev.notmarra.inarifarms.gui

import dev.notmarra.inarifarms.data.BlockDataManager
import dev.notmarra.inarifarms.items.ItemManager
import dev.notmarra.inarifarms.stations.Station
import dev.notmarra.inarifarms.stations.StationLevel
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class StationGui(
    val station: Station,
    val stationLevel: StationLevel,
    val block: Block,
    private val itemManager: ItemManager,
    private val blockDataManager: BlockDataManager,
) : InventoryHolder {
    private val mm = MiniMessage.miniMessage()

    private val inventory: Inventory = Bukkit.createInventory(
        this,
        27,
        mm.deserialize(station.displayName)
    )

    val seedSlotIndices = (0 until stationLevel.seedSlots).toList()
    val storageSlotIndices = (9 until 9 + stationLevel.storageSlots).toList()

    init {
        buildGui()
        loadFromBlock()
    }

    override fun getInventory(): Inventory = inventory

    fun open(player: Player) = player.openInventory(inventory)

    fun saveToBlock() {
        for (i in seedSlotIndices) {
            blockDataManager.setSeedSlotItem(block, i, inventory.getItem(i))
        }
        for (i in storageSlotIndices) {
            blockDataManager.setStorageSlotItem(block, i - 9, inventory.getItem(i))
        }
    }

    fun isSeedItem(item: ItemStack): Boolean {
        val pdc = item.itemMeta?.persistentDataContainer ?: return false
        val id = pdc.get(itemManager.customItemKey, PersistentDataType.STRING) ?: return false
        return !id.contains("station")
    }

    private fun loadFromBlock() {
        for (i in seedSlotIndices) {
            val saved = blockDataManager.getSeedSlotItem(block, i)
            if (saved != null) inventory.setItem(i, saved)
        }
        for (i in storageSlotIndices) {
            val saved = blockDataManager.getStorageSlotItem(block, i - 9)
            if (saved != null) inventory.setItem(i, saved)
        }
    }

    private fun buildGui() {
        // Výplň fillerem
        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.also { it.displayName(mm.deserialize("<!italic><gray> ")) }
        }
        for (i in 0 until 27) inventory.setItem(i, filler)

        // Seed sloty
        for (i in seedSlotIndices) {
            inventory.setItem(i, ItemStack(Material.LIME_STAINED_GLASS_PANE).apply {
                itemMeta = itemMeta?.also {
                    it.displayName(mm.deserialize("<!italic><green>Seed Slot ${i + 1}"))
                    it.lore(listOf(mm.deserialize("<!italic><gray>Vlož inari semínko")))
                }
            })
        }

        // Storage sloty
        for (i in storageSlotIndices) {
            inventory.setItem(i, ItemStack(Material.BROWN_STAINED_GLASS_PANE).apply {
                itemMeta = itemMeta?.also {
                    it.displayName(mm.deserialize("<!italic><gold>Storage Slot ${i - 8}"))
                }
            })
        }

        // Info slot
        inventory.setItem(26, ItemStack(Material.BOOK).apply {
            itemMeta = itemMeta?.also { meta ->
                meta.displayName(mm.deserialize("<!italic><yellow>${station.displayName}"))
                meta.lore(listOf(
                    mm.deserialize("<!italic><gray>Level: <white>${stationLevel.level}/<gold>${station.maxLevel()}"),
                    mm.deserialize("<!italic><gray>Seed sloty: <white>${stationLevel.seedSlots}"),
                    mm.deserialize("<!italic><gray>Storage sloty: <white>${stationLevel.storageSlots}"),
                    mm.deserialize("<!italic><gray>Rychlost růstu: <white>${stationLevel.growthSpeedMultiplier}x"),
                ))
            }
        })
    }
}