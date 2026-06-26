package dev.notmarra.inarifarms.crops

import org.bukkit.plugin.Plugin
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

class CropRegistry(private val plugin: Plugin) {
    private val crops = mutableMapOf<String, Crop>()

    fun loadAllCrops() {
        crops.clear()

        val plantsDir = File(plugin.dataFolder, "plants")
        if (!plantsDir.exists()) {
            plantsDir.mkdirs()
            plugin.saveResource("plants/tomato.yml", false)
        }

        val files = plantsDir.listFiles { _, name -> name.endsWith(".yml") } ?: return

        for (file in files) {
            try {
                val loader = YamlConfigurationLoader.builder()
                    .path(file.toPath())
                    .build()

                val node = loader.load()

                val crop = node.get(Crop::class.java)

                if (crop != null && crop.id.isNotEmpty()) {
                    crops[crop.fullId] = crop
                    plugin.logger.info("Loaded crop ${crop.id}")
                } else {
                    plugin.logger.warning("File ${file.name} does not have ID")
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load crop ${file.name} : ${e.message}")
            }
        }
        plugin.logger.info("Loaded ${crops.size} plants")
    }

    fun getCrop(id: String): Crop? = crops[id]

    fun getAllCrops(): Collection<Crop> = crops.values
}