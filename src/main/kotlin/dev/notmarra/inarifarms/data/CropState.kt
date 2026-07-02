package dev.notmarra.inarifarms.data

data class CropState(
    val cropTypeId: String,
    var currentStage: Int,
    var nextGrowthTime: Long = 0,
)