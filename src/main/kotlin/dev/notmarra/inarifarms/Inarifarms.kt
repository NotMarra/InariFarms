package dev.notmarra.inarifarms

import dev.notmarra.inarifarms.crops.CropRegistry
import dev.notmarra.inarifarms.data.CropDataManager
import dev.notmarra.inarifarms.display.HoverDisplayManager
import dev.notmarra.inarifarms.items.ItemManager
import dev.notmarra.inarifarms.listeners.CropPlantListener
import dev.notmarra.inarifarms.utils.CommandBuilder
import dev.notmarra.inarifarms.utils.config.ConfigManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class Inarifarms : JavaPlugin() {

    private lateinit var displayManager: HoverDisplayManager
    private lateinit var cropRegistry: CropRegistry
    lateinit var configManager: ConfigManager
        private set

    override fun onEnable() {
        configManager = ConfigManager(this)
        configManager.loadConfig()
        cropRegistry = CropRegistry(this)
        cropRegistry.loadAllCrops()

        val dataManager = CropDataManager(this)
        val itemManager = ItemManager(this)
        val commandBuilder = CommandBuilder(cropRegistry)

        displayManager = HoverDisplayManager(this, dataManager, cropRegistry)

        server.pluginManager.registerEvents(CropPlantListener(dataManager, itemManager, cropRegistry), this)

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()
            commands.register(commandBuilder.giveSeedCommand(itemManager))
        }

        logger.info("Inarifarms successfully loaded!")
    }

    override fun onDisable() {
        if (::displayManager.isInitialized) {
            displayManager.cleanup()
        }
        logger.info("Inarifarms successfully disabled!")
    }
}