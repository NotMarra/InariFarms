package dev.notmarra.inarifarms.gui

import dev.notmarra.inarifarms.data.BlockDataManager
import dev.notmarra.inarifarms.items.ItemManager
import dev.notmarra.inarifarms.stations.Station
import dev.notmarra.inarifarms.stations.StationLevel
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.item.BoundItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window

class StationGui(
    val station: Station,
    val stationLevel: StationLevel,
    val block: Block,
    private val itemManager: ItemManager,
    private val blockDataManager: BlockDataManager,
) {
    private val mm = MiniMessage.miniMessage()

    val storageSlotIndices = (9 until 9 + stationLevel.storageSlots).toList()

    private val seedInv = VirtualInventory(1)

    private val storedItems = mutableListOf<Item>()

    val gui: ScrollGui<Item>

    init {
        loadFromBlock()
        gui = buildScrollGui()
    }

    private fun loadFromBlock() {
        val savedSeed = blockDataManager.getSeedSlotItem(block)
        if (savedSeed != null) {
            seedInv.setItem(null, 0, savedSeed)
        }

        for (i in storageSlotIndices) {
            val dbSlot = i - 9
            val savedItemStack = blockDataManager.getStorageSlotItem(block, dbSlot)

            if (savedItemStack != null && savedItemStack.type != Material.AIR) {
                storedItems.add(buildClickableStorageItem(savedItemStack, dbSlot))
            }
        }
    }

    private fun buildClickableStorageItem(savedItemStack: ItemStack, dbSlot: Int): Item {
        lateinit var itemRef: Item

        itemRef = Item.builder()
            .setItemProvider(ItemBuilder(savedItemStack))
            .addClickHandler { _, event ->
                val clicker = event.player as Player

                val leftover = clicker.inventory.addItem(savedItemStack)

                if (leftover.isEmpty()) {
                    blockDataManager.setStorageSlotItem(block, dbSlot, null)

                    storedItems.remove(itemRef)

                    gui.setContent(storedItems)
                } else {
                    clicker.sendMessage(mm.deserialize("<red>You don't have enough space in your inventory!"))
                }
            }
            .build()

        return itemRef
    }

    private fun buildScrollGui(): ScrollGui<Item> {
        val lockConfig = station.gui.items["lock"]
        val lockItemBuilder = ItemBuilder(Material.valueOf(lockConfig?.material?.uppercase() ?: "BARRIER"))
        if (lockConfig != null) {
            if (lockConfig.name.isNotBlank()) lockItemBuilder.setName(mm.deserialize(lockConfig.name))
            if (lockConfig.lore.isNotEmpty()) lockItemBuilder.setLore(lockConfig.lore.map { mm.deserialize(it) })
        }
        val lockItem = Item.simple(lockItemBuilder)

        var encounteredSlots = 0
        val modifiedStructure = station.gui.structure.map { row ->
            val sb = StringBuilder()
            for (char in row) {
                if (char == '&') {
                    if (encounteredSlots < stationLevel.storageSlots) {
                        sb.append('&')
                    } else {
                        sb.append('L')
                    }
                    encounteredSlots++
                } else {
                    sb.append(char)
                }
            }
            sb.toString()
        }

        val builder = ScrollGui.itemsBuilder()
            .setStructure(*modifiedStructure.toTypedArray())
            .addIngredient('$', seedInv)
            .addIngredient('&', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('L', lockItem)

        station.gui.items.forEach { (symbolString, guiItem) ->
            if (symbolString.length == 1) {
                val char = symbolString.first()
                if (char == '&' || char == '$') return@forEach

                val itemBuilder = ItemBuilder(Material.valueOf(guiItem.material.uppercase()))
                if (guiItem.name.isNotBlank()) itemBuilder.setName(mm.deserialize(guiItem.name))
                if (guiItem.lore.isNotEmpty()) itemBuilder.setLore(guiItem.lore.map { mm.deserialize(it) })

                when (char) {
                    '»' -> {
                        val upConfig = station.gui.items["»"]!!
                        val upItem: BoundItem = BoundItem.scrollBuilder()
                            .setItemProvider(
                                ItemBuilder(Material.matchMaterial(upConfig.material) ?: Material.ARROW)
                                    .setName(mm.deserialize(upConfig.name))
                                    .setLore(upConfig.lore.map { mm.deserialize(it) })
                            )
                            .addClickHandler { _, gui, _ -> gui.line-- }
                            .build()
                        builder.addIngredient('»', upItem)
                    }
                    '«' -> {
                        val downConfig = station.gui.items["«"]!!
                        val downItem: BoundItem = BoundItem.scrollBuilder()
                            .setItemProvider(
                                ItemBuilder(Material.matchMaterial(downConfig.material) ?: Material.ARROW)
                                    .setName(mm.deserialize(downConfig.name))
                                    .setLore(downConfig.lore.map { mm.deserialize(it) })
                            )
                            .addClickHandler { _, gui, _ -> gui.line++ }
                            .build()
                        builder.addIngredient('«', downItem)
                    }
                    else -> {
                        builder.addIngredient(char, Item.simple(itemBuilder))
                    }
                }
            }
        }

        builder.setContent(storedItems)

        return builder.build()
    }

    fun open(player: Player) {
        val window: Window = Window.builder()
            .setTitle(mm.deserialize(station.gui.title))
            .setUpperGui(gui)
            .setViewer(player)
            .addCloseHandler {
                val currentSeed = seedInv.getItem(0)
                blockDataManager.setSeedSlotItem(block, currentSeed)
            }
            .build()

        window.open()
    }
}