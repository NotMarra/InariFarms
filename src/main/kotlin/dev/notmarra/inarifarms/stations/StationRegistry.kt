package dev.notmarra.inarifarms.stations

import org.bukkit.plugin.Plugin
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

class StationRegistry(private val plugin: Plugin) {
    private val stations = mutableMapOf<String, Station>()

    fun loadAllStations() {
        stations.clear()

        val stationsDir = File(plugin.dataFolder, "stations")
        if (!stationsDir.exists()) {
            stationsDir.mkdirs()
            plugin.saveResource("stations/growth_station.yml", false)
        }

        val files = stationsDir.listFiles { _, name -> name.endsWith(".yml") } ?: return

        for (file in files) {
            try {
                val loader = YamlConfigurationLoader.builder()
                    .path(file.toPath())
                    .build()

                val node = loader.load()

                val station = node.get(Station::class.java)

                if (station != null && station.id.isNotEmpty()) {
                    stations[station.fullId] = station
                    plugin.logger.info("Loaded station ${station.id}")
                } else {
                    plugin.logger.warning("File ${file.name} does not have ID")
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load station ${file.name} : ${e.message}")
            }
        }
        plugin.logger.info("Loaded ${stations.size} stations")
    }

    fun getStation(id: String): Station? = stations[id]

    fun getAllStations(): Collection<Station> = stations.values
}