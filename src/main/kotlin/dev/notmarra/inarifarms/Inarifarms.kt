package dev.notmarra.inarifarms

import dev.notmarra.inarifarms.crops.CropRegistry
import dev.notmarra.inarifarms.data.BlockDataManager
import dev.notmarra.inarifarms.items.ItemManager
import dev.notmarra.inarifarms.listeners.CropPlantListener
import dev.notmarra.inarifarms.listeners.StationBlockListener
import dev.notmarra.inarifarms.listeners.StationInteractListener
import dev.notmarra.inarifarms.stations.StationRegistry
import dev.notmarra.inarifarms.utils.CommandBuilder
import dev.notmarra.inarifarms.utils.config.ConfigManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.invui.InvUI

class Inarifarms : JavaPlugin() {

    private lateinit var cropRegistry: CropRegistry
    private lateinit var stationRegistry: StationRegistry
    lateinit var configManager: ConfigManager
        private set

    override fun onEnable() {
        configManager = ConfigManager(this)
        configManager.loadConfig()
        cropRegistry = CropRegistry(this)
        cropRegistry.loadAllCrops()
        stationRegistry = StationRegistry(this)
        stationRegistry.loadAllStations()
        InvUI.getInstance().setPlugin(this)

        val blockDataManager = BlockDataManager(this)
        val itemManager = ItemManager(this)
        val commandBuilder = CommandBuilder(this, cropRegistry, stationRegistry)

        server.pluginManager.registerEvents(CropPlantListener(itemManager, cropRegistry), this)
        server.pluginManager.registerEvents(StationBlockListener(blockDataManager, cropRegistry, itemManager, stationRegistry), this)
        server.pluginManager.registerEvents(StationInteractListener(blockDataManager, stationRegistry), this)

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()
            commands.register(commandBuilder.giveSeedCommand(itemManager))
            commands.register(commandBuilder.giveStationCommand(itemManager))
        }

        logger.info("Inarifarms successfully loaded!")
    }

    override fun onDisable() {
        logger.info("Inarifarms successfully disabled!")
    }
}