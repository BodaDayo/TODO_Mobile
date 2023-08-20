package com.rgbstudios.todomobile.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rgbstudios.todomobile.ui.fragments.OnboardingOneFragment
import com.rgbstudios.todomobile.ui.fragments.OnboardingThreeFragment
import com.rgbstudios.todomobile.ui.fragments.OnboardingTwoFragment

class OnboardingPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OnboardingOneFragment()
            1 -> OnboardingTwoFragment()
            2 -> OnboardingThreeFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
