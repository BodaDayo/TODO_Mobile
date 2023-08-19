package com.rgbstudios.todomobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rgbstudios.todomobile.data.Avatars
import com.rgbstudios.todomobile.databinding.DialogDiscardTaskBinding
import com.rgbstudios.todomobile.databinding.FragmentProfileBinding
import com.rgbstudios.todomobile.model.TaskViewModel
import com.rgbstudios.todomobile.model.UserDetails
import com.rgbstudios.todomobile.model.UserDetailsFromFirebase
import java.io.ByteArrayOutputStream


class ProfileFragment : Fragment() {
    private val sharedViewModel: TaskViewModel by activityViewModels()
    private lateinit var binding: FragmentProfileBinding
    private lateinit var newAvatar: Bitmap
    private var changesMade = false
    private var isImageExpandLayoutVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        changesMade = savedInstanceState?.getBoolean("changesMade") ?: false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.profileImageView.setOnClickListener {

            // Set the expandedImage with current newAvatar
            binding.expandedImage.setImageBitmap(newAvatar)

            // show the expanded profile image
            binding.imageExpandLayout.visibility = View.VISIBLE

            binding.editAvatar.setOnClickListener {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            isImageExpandLayoutVisible = true
        }

        binding.changeAvatar.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.saveButton.setOnClickListener {

            val name = binding.nameEditText.text.toString()
            val occupation = binding.occupationText.text.toString()

            if (changesMade) {
                sharedViewModel.updateUserDetails(name, occupation) { isSuccessful ->
                    if (isSuccessful) {
                        Toast.makeText(
                            context,
                            "Changes saved Successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        activity?.supportFragmentManager?.popBackStack()
                    } else {
                        Toast.makeText(
                            context,
                            "Something went wrong...\nTry Again!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                activity?.supportFragmentManager?.popBackStack()
            }
        }

        // Observe the userAvatarUrl LiveData and load the avatar using Glide
        sharedViewModel.userAvatarUrl.observe(viewLifecycleOwner) { avatarUrl ->

            // Set profile image and stop animation
            if (avatarUrl != null) {
                Glide.with(requireContext())
                    .asBitmap() // Load as a bitmap
                    .load(avatarUrl)
                    .circleCrop()
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            newAvatar = resource

                            // Update profileImage
                            Glide.with(requireContext())
                                .load(resource)
                                .circleCrop()
                                .into(binding.profileImageView)

                            // stop loading animation
                            binding.overlayView.visibility = View.GONE
                            binding.progressBar.visibility = View.GONE
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // Do nothing
                        }
                    })

            } else {
                // If avatarUrl is null, log it
                Log.d("ProfileFragment", "null avatarURl")

                // Stop the loading animation
                binding.overlayView.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
            }
        }

        // Observe userEmail LiveData
        sharedViewModel.userEmail.observe(viewLifecycleOwner) {
            binding.emailText.text = it
        }

        // Observe userDetails LiveData
        sharedViewModel.userDetailsFromFirebase.observe(viewLifecycleOwner) {
            binding.nameEditText.text = Editable.Factory.getInstance().newEditable(it.name)
            binding.occupationText.text =
                Editable.Factory.getInstance().newEditable(it.occupation)
        }

        // Set up nameText text listener
        binding.nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                changesMade = true
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Set up occupationText text listener
        binding.occupationText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                changesMade = true
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // custom up button
        binding.popBack.setOnClickListener {
            popBackStackManager()
        }

        // customize onBackPressed method
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (isImageExpandLayoutVisible) {
                binding.imageExpandLayout.visibility = View.GONE
                isImageExpandLayoutVisible = false
            } else {
                popBackStackManager()
            }
        }

    }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                binding.apply {
                    cropImageView.setImageUriAsync(uri)
                    cropImageView.setAspectRatio(1, 1)

                    // show the cropping layout
                    imageCropLayout.visibility = View.VISIBLE

                    doneCrop.setOnClickListener {

                        val croppedBitmap = cropImageView.getCroppedImage()
                        val compressedBitmap = croppedBitmap?.let { it1 -> compressBitmap(it1) }
                        compressedBitmap?.let { newAvatar = it }

                        // display progress bar while waiting to complete upload
                        progressBar.visibility = View.VISIBLE
                        overlayView.visibility = View.VISIBLE

                        // close the cropping layer and
                        imageCropLayout.visibility = View.GONE
                        imageExpandLayout.visibility = View.GONE
                        isImageExpandLayoutVisible = false

                        //upload the selected image to firebase Storage
                        sharedViewModel.uploadUserAvatar(newAvatar) { isSuccessful ->
                            if (isSuccessful) {
                                Toast.makeText(
                                    context,
                                    "Profile image changed Successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(context, "Image upload failed!", Toast.LENGTH_SHORT)
                                    .show()

                                // Stop the loading animation
                                overlayView.visibility = View.GONE
                                progressBar.visibility = View.GONE
                            }
                        }
                    }
                    cancelCrop.setOnClickListener {
                        imageCropLayout.visibility = View.GONE
                    }

                    rotateCrop.setOnClickListener {
                        cropImageView.rotateImage(90)
                    }
                }
            }
        }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            80,
            outputStream
        ) // Adjust compression quality as needed
        val compressedByteArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(compressedByteArray, 0, compressedByteArray.size)
    }

    private fun popBackStackManager() {
        if (changesMade) {
            showDiscardDialog()
        } else {
            // If no changes, simply pop back stack
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    private fun showDiscardDialog() {
        val dialogBinding = DialogDiscardTaskBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.discardQ.text = getString(R.string.discard_changes)
        dialogBinding.btnDiscardConfirm.setOnClickListener {

            // Reset changesMade flag
            changesMade = false

            // Dismiss the dialog
            dialog.dismiss()

            // pop back stack
            activity?.supportFragmentManager?.popBackStack()
        }

        dialogBinding.btnDiscardCancel.setOnClickListener {
            // Dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the state of imageExpandLayout visibility
        outState.putBoolean("imageExpandLayoutVisible", isImageExpandLayoutVisible)
        outState.putBoolean("changesMade", changesMade)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Restore the state of isImageExpandLayoutVisible and changesMade
        isImageExpandLayoutVisible =
            savedInstanceState?.getBoolean("imageExpandLayoutVisible") ?: false
        changesMade = savedInstanceState?.getBoolean("changesMade") ?: false
        if (isImageExpandLayoutVisible) {
            binding.imageExpandLayout.visibility = View.VISIBLE
        }
    }
}