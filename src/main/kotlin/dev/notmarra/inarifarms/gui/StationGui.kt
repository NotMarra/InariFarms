package dev.notmarra.inarifarms.gui

import dev.notmarra.inarifarms.data.BlockDataManager
import dev.notmarra.inarifarms.items.ItemManager
import dev.notmarra.inarifarms.stations.Station
import dev.notmarra.inarifarms.stations.StationLevel
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.setTitle

class StationGui(
    val station: Station,
    val stationLevel: StationLevel,
    val block: Block,
    private val itemManager: ItemManager,
    private val blockDataManager: BlockDataManager,
) {
    private val mm = MiniMessage.miniMessage()

    val filler: Item = Item.builder()
        .setItemProvider(ItemBuilder(Material.BLACK_STAINED_GLASS_PANE))
        .addClickHandler { item, click -> println("test")  }
        .build()

    val gui: Gui = Gui.builder()
        .setStructure("xxxxxxxxx")
        .addIngredient('x', filler)
        .build()


    fun open(player: Player) {
        val window: Window = Window.builder()
            .setTitle("Station")
            .setUpperGui(gui)
            .setViewer(player)
            .build()

        window.open()
    }


}