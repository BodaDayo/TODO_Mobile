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
        "grocery" to R.drawable.shopping_cart,
        "movies" to R.drawable.movies,
        "finance" to R.drawable.cash,
        "travel" to R.drawable.travel,
        "gaming" to R.drawable.gamepad,
        "albums" to R.drawable.album,
        "airplane" to R.drawable.airplane,
        "alarm" to R.drawable.alarm_clock,
        "art" to R.drawable.artist,
        "bicycle" to R.drawable.bicycle,
        "book" to R.drawable.book,
        "cake" to R.drawable.cake,
        "basketball" to R.drawable.basketball,
        "car" to R.drawable.car,
        "film" to R.drawable.film,
        "coffee" to R.drawable.coffee,
        "world" to R.drawable.globe,
        "key" to R.drawable.key,
        "laptop" to R.drawable.laptop,
        "food" to R.drawable.bread,
        "light" to R.drawable.lightbulb,
        "explore" to R.drawable.map,
        "phone" to R.drawable.phone,
        "event" to R.drawable.calendar,
        "cook" to R.drawable.cook,
        "puzzle" to R.drawable.puzzle_piece,
        "rocket" to R.drawable.rocket,
        "luggage" to R.drawable.suitcase,
        "environment" to R.drawable.tree,
        "trophy" to R.drawable.trophy
    )

    fun getDefaultIcons(): List<String> {
        val iconsList = iconPairs.toMutableMap()

        // Remove the first and second pair
        iconsList.remove("add_new")
        iconsList.remove("default")

        // Get the first 9 keys
        val defaultKeys = iconsList.keys.take(9)

        return defaultKeys.toList()
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

    private val emojiTriples = listOf(
        Triple("poor", R.drawable.emo5, R.color.poor),
        Triple("fair", R.drawable.emo4, R.color.fair),
        Triple("good", R.drawable.emo3, R.color.good),
        Triple("very_good", R.drawable.emo2, R.color.very_good),
        Triple("excellent", R.drawable.emo1, R.color.excellent),
    )

    fun getEmojiList(): List<Triple<String, Int, Int>> {
        return emojiTriples
    }

}