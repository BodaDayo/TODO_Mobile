package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.databinding.FragmentEditTaskBinding
import com.rgbstudios.todomobile.databinding.FragmentSettingsBinding
import com.rgbstudios.todomobile.ui.adapters.CategoryColorAdapter
import com.rgbstudios.todomobile.utils.ColorManager
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.utils.ToastManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel


class SettingsFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels()
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var fragmentContext: Context
    private val dialogManager = DialogManager()
    private val toastManager = ToastManager()
    private val firebase = FirebaseAccess()
    private val thisFragment = this
    private val colorManager = ColorManager()
    private val colors = colorManager.getAllColors()
    private val colorList = mutableListOf(PRIMARY)


    // Variable to store the selected color
    var selectedThemeColor: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        fragmentContext = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add the list of colors to the default primary color
        colorList.addAll(colors)

        binding.apply {
            val themeColorAdapter =
                CategoryColorAdapter(
                    colorList,
                    colorManager,
                    object : CategoryColorAdapter.ColorClickListener {
                        override fun onColorClick(colorIdentifier: String) {
                            // Handle the color click event and update the selected color
                            selectedThemeColor = colorIdentifier
                            appThemeColorLayout.visibility = View.GONE

                            // Set the background for categoryColorView
                            themeColorView.setBackgroundResource(R.drawable.circular_primary_background)

                            val colorPair = colorManager.getColorMapping(colorIdentifier)

                            // Set the background tint for categoryColorView
                            themeColorView.backgroundTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(fragmentContext, colorPair.second))
                        }
                    }
                )
            appThemeColorRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

            // Set the adapter for the colorRecyclerView
            appThemeColorRecyclerView.adapter = themeColorAdapter


            changeAppThemeLayout.setOnClickListener {
                appThemeColorLayout.visibility = View.VISIBLE
            }
        }

    }

    companion object {
        private const val PRIMARY = "primary"
    }
}