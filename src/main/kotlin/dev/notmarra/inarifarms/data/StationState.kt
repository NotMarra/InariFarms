package dev.notmarra.inarifarms.data

import org.bukkit.inventory.ItemStack

data class StationState(
    val stationTypeId: String,
    val level: Int,
    val cropStates: List<CropState>,
    val storageItems: List<ItemStack>
)