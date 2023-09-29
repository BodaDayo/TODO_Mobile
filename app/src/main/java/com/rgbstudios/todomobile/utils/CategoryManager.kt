package com.rgbstudios.todomobile.utils

import com.google.firebase.analytics.FirebaseAnalytics.Event.SEARCH
import com.rgbstudios.todomobile.data.entity.CategoryEntity
import java.util.UUID
import kotlin.reflect.KTypeProjection.Companion.STAR

class CategoryManager {

    private val colorManager = ColorManager()
    private val iconManager = IconManager()

    private val icons = iconManager.getDefaultIcons()

    val newCategory =
        CategoryEntity(CREATE, NAME, ICON, colorManager.newPair)

    val specialCategoryNames = listOf( UNCOMPLETED, COMPLETED, SEARCH, STAR)

    fun getDefaultCategories(): List<CategoryEntity> {
        return icons.map { name ->
            val categoryId = UUID.randomUUID().toString()
            val categoryName = name.replaceFirstChar { it.uppercase() }
            val categoryColorIdentifier = colorManager.getRandomColorPair()

            CategoryEntity(
                categoryId = categoryId,
                categoryName = categoryName,
                categoryIconIdentifier = name,
                categoryColorIdentifier = categoryColorIdentifier
            )
        }
    }

    companion object {
        private const val CREATE = "create_new"
        private const val NAME = "Add New"
        private const val ICON = "add_new"
        private const val STAR = "Favorites"
        private const val SEARCH = "Search Results"
        private const val UNCOMPLETED = "uncompleted"
        private const val COMPLETED = "completed"
    }

}