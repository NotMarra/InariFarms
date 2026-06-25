package dev.notmarra.inarifarms.data

data class CropState(
    val cropTypeId: String,
    var currentStage: Int,
    var currentMoisture: Int,
    var nextGrowthTime: Long = 0,
)