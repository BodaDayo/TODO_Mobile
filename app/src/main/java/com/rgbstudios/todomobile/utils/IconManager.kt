package com.rgbstudios.todomobile.utils

import com.rgbstudios.todomobile.R

class IconManager {

    private val iconPairs = mapOf(
        "add_new" to R.drawable.add,
        "default" to R.drawable.tag,
        "home" to R.drawable.home_icon,
        "school" to R.drawable.school,
        "sport" to R.drawable.fitness,
        "health" to R.drawable.health,
        "music" to R.drawable.music,
        "social" to R.drawable.social,
        "grocery" to R.drawable.grocery,
        "movies" to R.drawable.movies,
        "travel" to R.drawable.travel,
    )

    fun getDefaultIcons(): List<String> {
        val iconsList = iconPairs.toMutableMap()

        // Remove the first and second pair
        iconsList.remove("add_new")
        iconsList.remove("default")

        return iconsList.keys.toList()
    }

    fun getAllIcons(): List<String> {
        val keys = iconPairs.keys.toMutableList()
        keys.remove("add_new")
        keys.remove("default")
        keys.add("default")
        return keys
    }

    fun getDefaultIcon(): String {
        return iconPairs.keys.first { it == "default" }
    }

    fun getIconDrawableResource(iconIdentifier: String): Int {
        return iconPairs[iconIdentifier] ?: R.drawable.tag
    }

}