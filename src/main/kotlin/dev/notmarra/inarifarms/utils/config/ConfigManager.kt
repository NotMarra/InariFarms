package dev.notmarra.inarifarms.utils.config

import org.bukkit.plugin.Plugin
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

class ConfigManager(private val plugin: Plugin) {
    private val configFile = File(plugin.dataFolder, "config.yml")
    private lateinit var loader: YamlConfigurationLoader

    lateinit var config: MainConfig
        private set

    fun loadConfig() {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }

        loader = YamlConfigurationLoader.builder()
            .path(configFile.toPath())
            .build()

        try {
            val root = loader.load()

            config = root.get(MainConfig::class.java) ?: MainConfig()

            root.set(MainConfig::class.java, config)
            loader.save(root)
        } catch (e: Exception) {
            plugin.logger.severe(e.message)
            config = MainConfig()
        }
    }
}