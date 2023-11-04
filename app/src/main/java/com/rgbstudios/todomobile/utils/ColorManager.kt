package com.rgbstudios.todomobile.utils

import com.rgbstudios.todomobile.R

class ColorManager {
    private val allColorPairs = listOf(
        "grey" to Pair(R.color.my_darker_grey, R.color.circular_progress_overlay_background),
        "gold" to Pair(R.color.light_gold, R.color.dark_orange),
        "turquoise" to Pair(R.color.light_dark_turquoise, R.color.dark_dark_cyan),
        "pink" to Pair(R.color.light_hot_pink, R.color.dark_deep_pink),
        "green" to Pair(R.color.light_lime_green, R.color.dark_green),
        "orchid" to Pair(R.color.light_medium_orchid, R.color.dark_purple),
        "salmon" to Pair(R.color.light_light_salmon, R.color.dark_orange_red),
        "blue" to Pair(R.color.light_sky_blue, R.color.dark_royal_blue),
        "coral" to Pair(R.color.light_light_coral, R.color.dark_indian_red),
        "primary" to Pair(R.color.myPrimaryVariant, R.color.myPrimary)
    )

    private val colorPairs =
        allColorPairs.filterNot { it.first == "grey" || it.first == "primary" }.shuffled().toMutableList()


    val newPair = allColorPairs.first().first

    fun getRandomColorPair(): String {
        if (colorPairs.isEmpty()) {
            colorPairs.addAll(
                allColorPairs.filterNot { it.first == "grey" || it.first == "primary" }.shuffled()
            )
        }

        val colorPair = colorPairs.removeAt(0)
        return colorPair.first
    }

    fun getAllColors(): List<String> {
        return allColorPairs.map { it.first }.filterNot { it == "grey" || it == "primary" }
    }

    fun getDefaultColor(): String {
        return allColorPairs.map { it.first }.first { it == "orchid" }
    }

    fun getColorMapping(colorName: String): Pair<Int, Int> {
        val colorPair = allColorPairs.find { it.first == colorName }
        return colorPair?.second ?: Pair(R.color.light_medium_orchid, R.color.dark_purple)
    }
}
