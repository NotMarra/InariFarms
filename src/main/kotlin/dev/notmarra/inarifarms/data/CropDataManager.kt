package dev.notmarra.inarifarms.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class CropDataManager(plugin: Plugin) : Listener {
    private val gson = Gson()
    private val chunkDataKey = NamespacedKey(plugin, "inari_chunk_crops")
    private val cache = HashMap<String, CachedChunk>()

    private fun chunkKey(chunk: Chunk): String = "${chunk.world.uid}:${chunk.x}:${chunk.z}"

    private fun loadFromPDC(chunk: Chunk): HashMap<String, CropState> {
        val json = chunk.persistentDataContainer.get(chunkDataKey, PersistentDataType.STRING) ?: return HashMap()
        val type = object : TypeToken<HashMap<String, CropState>>() {}.type
        return gson.fromJson<HashMap<String, CropState>>(json, type) ?: HashMap()
    }

    private fun persistToPDC(chunk: Chunk, crops: HashMap<String, CropState>) {
        val json = gson.toJson(crops)
        chunk.persistentDataContainer.set(chunkDataKey, PersistentDataType.STRING, json)
    }

    fun getChunkCrops(chunk: Chunk): HashMap<String, CropState> {
        val key = chunkKey(chunk)
        return cache.getOrPut(key) { CachedChunk(chunk, loadFromPDC(chunk), dirty = false) }.crops
    }

    fun saveChunkCrops(chunk: Chunk, crops: HashMap<String, CropState>) {
        val key = chunkKey(chunk)
        cache[key] = CachedChunk(chunk, crops, dirty = true)
    }

    fun invalidateChunk(chunk: Chunk) {
        cache.remove(chunkKey(chunk))
    }

    fun flushChunk(chunk: Chunk) {
        val key = chunkKey(chunk)
        cache[key]?.let { entry ->
            persistToPDC(entry.chunk, entry.crops)
            entry.dirty = false
        }
    }

    @Suppress("unused")
    fun flushAll() {
        cache.values.forEach { entry ->
            flushChunk(entry.chunk)
        }
    }

    fun getCropData(block: Block): CropState? {
        return getChunkCrops(block.chunk)[getBlockKey(block)]
    }

    fun saveCropData(block: Block, state: CropState) {
        val chunk = block.chunk
        val chunkCrops = getChunkCrops(chunk)
        chunkCrops[getBlockKey(block)] = state
        cache[chunkKey(chunk)]?.dirty = true
    }

    private fun getBlockKey(block: Block): String = "${block.x and 15},${block.y},${block.z and 15}"

    @EventHandler
    fun onChunkUnload(event: ChunkUnloadEvent) {
        flushAll()
        invalidateChunk(event.chunk)
    }

    private data class CachedChunk(
        val chunk: Chunk,
        val crops: HashMap<String, CropState>,
        var dirty: Boolean,
    )
}