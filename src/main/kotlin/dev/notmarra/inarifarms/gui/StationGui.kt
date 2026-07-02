package dev.notmarra.inarifarms.gui

import dev.notmarra.inarifarms.crops.Crop
import dev.notmarra.inarifarms.crops.CropRegistry
import dev.notmarra.inarifarms.crops.GrowthEngine
import dev.notmarra.inarifarms.data.BlockDataManager
import dev.notmarra.inarifarms.data.CropState
import dev.notmarra.inarifarms.items.ItemManager
import dev.notmarra.inarifarms.stations.Station
import dev.notmarra.inarifarms.stations.StationLevel
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
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
    private val cropRegistry: CropRegistry,
) {
    private val mm = MiniMessage.miniMessage()

    val storageSlotIndices = (9 until 9 + stationLevel.storageSlots).toList()

    private val seedInv = VirtualInventory(1)
    private val infoInv = VirtualInventory(1)

    // Keyed by db slot index so harvested items can update/remove a specific
    // slot's displayed Item without scanning the whole list.
    private val storedItemsBySlot = linkedMapOf<Int, Item>()
    private val storedItems: MutableList<Item> get() = storedItemsBySlot.values.toMutableList()

    val gui: ScrollGui<Item>

    init {
        loadFromBlock()
        gui = buildScrollGui()

        infoInv.addPreUpdateHandler { event ->
            event.isCancelled = true
        }

        seedInv.addPostUpdateHandler {
            val currentSeed = seedInv.getItem(0)

            if (currentSeed == null || currentSeed.type == Material.AIR) {
                infoInv.setItem(null, 0, buildEmptyInfoItem())
            } else {
                val cropId = readCropId(currentSeed)
                val crop = cropId?.let { cropRegistry.getCrop(it) }

                if (crop == null) {
                    infoInv.setItem(null, 0, buildEmptyInfoItem())
                } else {
                    val state = blockDataManager.getCropState(block)
                        ?: CropState(cropTypeId = crop.fullId, currentStage = 0, nextGrowthTime = 0L)

                    infoInv.setItem(null, 0, buildGrowthInfoItem(crop, state))
                }
            }
        }
    }

    private fun loadFromBlock() {
        processGrowthAndHarvest()

        for (i in storageSlotIndices) {
            val dbSlot = i - 9
            val savedItemStack = blockDataManager.getStorageSlotItem(block, dbSlot)

            if (savedItemStack != null && savedItemStack.type != Material.AIR) {
                storedItemsBySlot[dbSlot] = buildClickableStorageItem(savedItemStack, dbSlot)
            }
        }
    }

    /**
     * Pull-based growth: simulates everything that happened since the last
     * time this station's GUI was opened (stored via CropState.nextGrowthTime),
     * including multiple auto-harvest -> auto-replant cycles if the seed slot
     * has a stack of seeds. Harvested crops are deposited straight into the
     * storage slots - this is what makes it work as an AFK farm, no scheduler
     * needed.
     */
    private fun processGrowthAndHarvest() {
        val savedSeed = blockDataManager.getSeedSlotItem(block) ?: run {
            infoInv.setItem(null, 0, buildEmptyInfoItem())
            return
        }

        val cropId = readCropId(savedSeed)
        val crop = cropId?.let { cropRegistry.getCrop(it) }

        if (crop == null) {
            // Untagged / unrecognized seed item - nothing we can simulate,
            // just show it as-is (e.g. a raw vanilla item someone dropped in).
            seedInv.setItem(null, 0, savedSeed)
            infoInv.setItem(null, 0, buildEmptyInfoItem())
            return
        }

        val matureSample = itemManager.createMature(crop)
        val freeCapacity = computeFreeStorageCapacity(matureSample)
        val startWater = blockDataManager.getStationWater(block)

        val existingState = blockDataManager.getCropState(block)
            ?: CropState(cropTypeId = crop.fullId, currentStage = 0, nextGrowthTime = 0L)

        val result = GrowthEngine.advance(
            state = existingState,
            crop = crop,
            speedMultiplier = stationLevel.growthSpeedMultiplier,
            seedCount = savedSeed.amount,
            freeStorageCapacity = freeCapacity,
            stationWater = startWater
        )

        if (result.harvestedCount > 0) {
            depositHarvest(matureSample, result.harvestedCount)
        }

        if (result.cropState == null) {
            // Last seed in the stack got consumed - nothing left to grow.
            blockDataManager.removeCropState(block)
            blockDataManager.setSeedSlotItem(block, null)
            // seedInv slot stays empty, waiting for the player to add more seeds.
            seedInv.setItem(null, 0, null)
            infoInv.setItem(null, 0, buildEmptyInfoItem())
        } else {
            blockDataManager.setCropState(block, result.cropState)
            val updatedSeed = savedSeed.clone().apply { amount = result.remainingSeedCount }
            blockDataManager.setSeedSlotItem(block, updatedSeed)
            seedInv.setItem(null, 0, updatedSeed)
            infoInv.setItem(null, 0, buildGrowthInfoItem(crop, existingState))
        }

        blockDataManager.setStationWater(block, result.remainingStationWater)
    }

    /**
     * Clones the (persisted, clean) seed item and appends live growth-progress
     * lore on top of it - stage, countdown to next stage, or a water/storage
     * warning depending on [GrowthEngine.growthStatus]. This display copy is
     * never persisted; it's rebuilt fresh from crop.growthInfo every time the
     * GUI loads.
     */
    private fun buildGrowthInfoItem(crop: Crop, state: CropState): ItemStack {
        val currentWater = blockDataManager.getStationWater(block)
        val status = GrowthEngine.growthStatus(state, crop, currentWater)

        val template = when (status) {
            GrowthEngine.GrowthStatus.WAITING_WATER -> station.gui.statusLoreConfig.waitingWater
            GrowthEngine.GrowthStatus.WAITING_STORAGE -> station.gui.statusLoreConfig.waitingStorage
            GrowthEngine.GrowthStatus.GROWING -> station.gui.statusLoreConfig.growing
        }

        val placeholders = mapOf(
            "%stage%" to state.currentStage.toString(),
            "%maxStage%" to crop.maxGrowthStage.toString(),
            "%timeLeft%" to formatDuration(GrowthEngine.remainingMillis(state))
        )

        val infoItemConfig = station.gui.items["!"]
        val material = Material.matchMaterial(infoItemConfig?.material ?: "PAPER") ?: Material.PAPER

        val display = ItemStack(material)
        val meta = display.itemMeta

        if (infoItemConfig != null && infoItemConfig.name.isNotBlank()) {
            meta.displayName(mm.deserialize(infoItemConfig.name))
        } else {
            meta.displayName(mm.deserialize("<green>Growth Information"))
        }

        val growthLore = template.map { mm.deserialize(replacePlaceholders(it, placeholders)) }
        meta.lore(growthLore)

        display.itemMeta = meta
        return display
    }

    private fun buildEmptyInfoItem(): ItemStack {
        val infoItemConfig = station.gui.items["!"]
        val material = Material.matchMaterial(infoItemConfig?.material ?: "PAPER") ?: Material.PAPER

        val display = ItemStack(material)
        val meta = display.itemMeta

        if (infoItemConfig != null && infoItemConfig.name.isNotBlank()) {
            meta.displayName(mm.deserialize(infoItemConfig.name))
        } else {
            meta.displayName(mm.deserialize("<red>Žádná plodina"))
        }

        val emptyLore = station.gui.statusLoreConfig.empty.map { mm.deserialize(it) }
        meta.lore(emptyLore)

        display.itemMeta = meta
        return display
    }

    private fun replacePlaceholders(text: String, placeholders: Map<String, String>): String {
        var result = text
        placeholders.forEach { (key, value) -> result = result.replace(key, value) }
        return result
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    /** How many more harvested items currently fit into the station's storage slots. */
    private fun computeFreeStorageCapacity(matureSample: ItemStack): Int {
        var free = 0
        val maxStack = matureSample.maxStackSize
        for (slot in 0 until stationLevel.storageSlots) {
            val existing = blockDataManager.getStorageSlotItem(block, slot)
            free += when {
                existing == null -> maxStack
                existing.isSimilar(matureSample) -> (maxStack - existing.amount).coerceAtLeast(0)
                else -> 0 // slot occupied by something else, no room here
            }
        }
        return free
    }

    /**
     * Writes `count` harvested items into storage slots directly via
     * BlockDataManager - stacking onto existing matching stacks first, then
     * filling empty slots. The display (storedItemsBySlot) is rebuilt right
     * after this by the normal loadFromBlock loop, so no UI work happens here.
     */
    private fun depositHarvest(matureSample: ItemStack, count: Int) {
        var remaining = count
        val maxStack = matureSample.maxStackSize

        for (slot in 0 until stationLevel.storageSlots) {
            if (remaining <= 0) break
            val existing = blockDataManager.getStorageSlotItem(block, slot)

            when {
                existing == null -> {
                    val toPlace = remaining.coerceAtMost(maxStack)
                    val newStack = matureSample.clone().apply { amount = toPlace }
                    blockDataManager.setStorageSlotItem(block, slot, newStack)
                    remaining -= toPlace
                }
                existing.isSimilar(matureSample) && existing.amount < maxStack -> {
                    val space = maxStack - existing.amount
                    val toAdd = remaining.coerceAtMost(space)
                    existing.amount += toAdd
                    blockDataManager.setStorageSlotItem(block, slot, existing)
                    remaining -= toAdd
                }
            }
        }
        // If `remaining > 0` here, storage filled up between capacity check and
        // deposit (shouldn't normally happen within a single GUI load, but we
        // don't crash on it - the leftover simply wasn't harvested this pass).
    }

    private fun readCropId(seed: ItemStack): String? =
        seed.itemMeta?.persistentDataContainer?.get(itemManager.customItemKey, PersistentDataType.STRING)

    private fun buildClickableStorageItem(savedItemStack: ItemStack, dbSlot: Int): Item {
        lateinit var itemRef: Item

        itemRef = Item.builder()
            .setItemProvider(ItemBuilder(savedItemStack))
            .addClickHandler { _, event ->
                val clicker = event.player as Player

                val leftover = clicker.inventory.addItem(savedItemStack)

                if (leftover.isEmpty()) {
                    blockDataManager.setStorageSlotItem(block, dbSlot, null)

                    storedItemsBySlot.remove(dbSlot)

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
            .addIngredient('!', infoInv)
            .addIngredient('&', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('L', lockItem)

        station.gui.items.forEach { (symbolString, guiItem) ->
            if (symbolString.length == 1) {
                val char = symbolString.first()
                if (char == '&' || char == '$' || char == '!') return@forEach

                val itemBuilder = ItemBuilder(Material.valueOf(guiItem.material.uppercase()))
                if (guiItem.name.isNotBlank()) itemBuilder.setName(mm.deserialize(guiItem.name))
                if (guiItem.lore.isNotEmpty()) itemBuilder.setLore(guiItem.lore.map { mm.deserialize(it) })

                when (char) {
                    '?' -> {
                        val currentWater = blockDataManager.getStationWater(block)

                        val placeholders = mapOf(
                            "%level%" to stationLevel.level.toString(),
                            "%maxLevel%" to station.levels.size.toString(),
                            "%slots%" to stationLevel.storageSlots.toString(),
                            "%multiplier%" to stationLevel.growthSpeedMultiplier.toString(),
                            "%water%" to currentWater.toString(),
                            "%maxWater%" to stationLevel.maxWater.toString()
                        )
                        val statsItemBuilder = ItemBuilder(Material.valueOf(guiItem.material.uppercase()))
                        if (guiItem.name.isNotBlank()) {
                            statsItemBuilder.setName(mm.deserialize(replacePlaceholders(guiItem.name, placeholders)))
                        }
                        if (guiItem.lore.isNotEmpty()) {
                            statsItemBuilder.setLore(guiItem.lore.map { mm.deserialize(replacePlaceholders(it, placeholders)) })
                        }
                        builder.addIngredient('?', Item.simple(statsItemBuilder))
                    }
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
                val previousSeed = blockDataManager.getSeedSlotItem(block)

                blockDataManager.setSeedSlotItem(block, currentSeed)

                if (currentSeed == null || (previousSeed != null && !currentSeed.isSimilar(previousSeed))) {
                    blockDataManager.removeCropState(block)
                }
            }
            .build()

        window.open()
    }
}