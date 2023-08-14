package com.rgbstudios.todomobile

import android.content.Context
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.rgbstudios.todomobile.databinding.FragmentEditTaskBinding
import com.rgbstudios.todomobile.databinding.FragmentProfileBinding
import com.rgbstudios.todomobile.model.TaskDataFromFirebase
import com.rgbstudios.todomobile.model.TaskViewModel


class ProfileFragment : Fragment() {
    private val sharedViewModel: TaskViewModel by activityViewModels()
    private lateinit var binding: FragmentProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        data class UserDataSample(val name: String, val email: String, val occupation: String, val stats: String)

        val sampleUserData = UserDataSample("Ololade Mr Money", "mrmoney@gmail.com", "soldier", "4")
        binding.apply {
            nameEditText.text = Editable.Factory.getInstance().newEditable(sampleUserData.name)
            emailText.text = sampleUserData.email
            occupationText.text = Editable.Factory.getInstance().newEditable(sampleUserData.occupation)

            profileImageView.setOnClickListener {

            }
        }

    }
}