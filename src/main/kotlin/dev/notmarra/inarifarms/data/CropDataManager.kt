package dev.notmarra.inarifarms.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class CropDataManager(plugin: Plugin) {
    private val gson = Gson()
    private val chunkDataKey = NamespacedKey(plugin, "inari_chunk_crops")

    fun getCropData(block: Block): CropState? {
        val chunkCrops = getChunkCrops(block.chunk)
        val key = getBlockKey(block)
        val result = chunkCrops[key]

        return result
    }

    fun saveCropData(block: Block, state: CropState) {
        val chunk = block.chunk
        val chunkCrops = getChunkCrops(chunk)
        chunkCrops[getBlockKey(block)] = state

        val key = getBlockKey(block)

        val json = gson.toJson(chunkCrops)
        chunk.persistentDataContainer.set(chunkDataKey, PersistentDataType.STRING, json)
    }

    private fun getChunkCrops(chunk: Chunk): HashMap<String, CropState> {
        val container = chunk.persistentDataContainer
        val json = container.get(chunkDataKey, PersistentDataType.STRING) ?: return HashMap()
        val type = object : TypeToken<HashMap<String, CropState>>() {}.type
        val result: HashMap<String, CropState>? = gson.fromJson(json, type)
        val finalResult = result ?: HashMap()
        return finalResult
    }

    private fun getBlockKey(block: Block): String {
        return "${block.x and 15},${block.y},${block.z and 15}"
    }
}