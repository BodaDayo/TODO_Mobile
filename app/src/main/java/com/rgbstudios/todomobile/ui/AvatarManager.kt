package com.rgbstudios.todomobile.ui

import com.rgbstudios.todomobile.R

class AvatarManager {
    private val defaultAvatarsList = listOf(
        R.drawable.asset_1,
        R.drawable.asset_2,
        R.drawable.asset_3,
        R.drawable.asset_4,
        R.drawable.asset_5,
        R.drawable.asset_6,
        R.drawable.asset_7,
        R.drawable.asset_8,
        R.drawable.asset_9,
        R.drawable.asset_10,
        R.drawable.asset_11,
        R.drawable.asset_12,
    )
    val defaultAvatar = defaultAvatarsList.random()

}