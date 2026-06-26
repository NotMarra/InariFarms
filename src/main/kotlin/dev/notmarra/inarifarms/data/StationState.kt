package dev.notmarra.inarifarms.data

import org.bukkit.inventory.ItemStack

data class StationState(
    val stationTypeId: String,
    val level: Int,
    val seedSlots: List<SeedSlotState>,
    val storageItems: List<ItemStack>
)

data class SeedSlotState(
    val cropTypeId: String?,
    val currentStage: Int = 0,
    val nextGrowthTime: Long = 0
)
