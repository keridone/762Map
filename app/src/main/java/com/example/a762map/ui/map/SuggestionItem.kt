package com.example.a762map.ui.map

data class SuggestionItem(
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val poiId: String = ""
) {
    fun hasPoint(): Boolean = !lat.isNaN() && !lng.isNaN()
}
