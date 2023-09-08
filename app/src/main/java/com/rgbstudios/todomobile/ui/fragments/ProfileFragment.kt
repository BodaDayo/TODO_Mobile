package com.rgbstudios.todomobile.ui.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.databinding.DialogDiscardTaskBinding
import com.rgbstudios.todomobile.databinding.FragmentProfileBinding
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import com.rgbstudios.todomobile.viewmodel.TodoViewModelFactory
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.io.File


class ProfileFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels {
        TodoViewModelFactory(activity?.application as TodoMobileApplication)
    }
    private lateinit var binding: FragmentProfileBinding
    private lateinit var newAvatarBitmap: Bitmap
    private var changesMade = false
    private var isImageExpandLayoutVisible = false
    private var userDetailsSetFromViewModel = false
    private var selectedImageView: CircleImageView? = null
    private var selectedDefaultAvatar: Int? = null
    private val borderWidth = 6


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
            binding.expandedImage.setImageBitmap(newAvatarBitmap)

            // show the expanded profile image
            binding.imageExpandLayout.visibility = View.VISIBLE

            binding.editAvatar.setOnClickListener {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            // Highlight last selected default avatar option
            selectedImageView?.isSelected = true
            val borderColor = ContextCompat.getColor(requireContext(), R.color.myPrimary)
            selectedImageView?.borderColor = borderColor
            selectedImageView?.borderWidth = borderWidth

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

        // Observe userDetails LiveData
        sharedViewModel.currentUser.observe(viewLifecycleOwner) { user ->

            if (user != null) {

                // Set profile image and stop animation
                if (user.avatarFilePath != null) {

                    Glide.with(requireContext())
                        .asBitmap()
                        .load(File(user.avatarFilePath))
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                newAvatarBitmap = resource

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

                    // Stop the loading animation
                    binding.overlayView.visibility = View.GONE
                    binding.progressBar.visibility = View.GONE
                }

                // Set user email
                binding.emailText.text = user.email
                binding.emailText.visibility = View.VISIBLE

                // Set user details
                if (!userDetailsSetFromViewModel && user.name != null && user.occupation != null) {
                    binding.nameEditText.text =
                        Editable.Factory.getInstance().newEditable(user.name)
                    binding.occupationText.text =
                        Editable.Factory.getInstance().newEditable(user.occupation)
                    userDetailsSetFromViewModel = true
                    changesMade = false
                }
            }
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
                Log.d("aaaa", "name changed")
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
                Log.d("aaaa", "occupation changed")
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

        // Set up click listeners for default avatars
        binding.circleImageView1.setOnClickListener { onCircleImageViewClicked(binding.circleImageView1) }
        binding.circleImageView2.setOnClickListener { onCircleImageViewClicked(binding.circleImageView2) }
        binding.circleImageView3.setOnClickListener { onCircleImageViewClicked(binding.circleImageView3) }
        binding.circleImageView4.setOnClickListener { onCircleImageViewClicked(binding.circleImageView4) }
        binding.circleImageView5.setOnClickListener { onCircleImageViewClicked(binding.circleImageView5) }
        binding.circleImageView6.setOnClickListener { onCircleImageViewClicked(binding.circleImageView6) }
        binding.circleImageView7.setOnClickListener { onCircleImageViewClicked(binding.circleImageView7) }
        binding.circleImageView8.setOnClickListener { onCircleImageViewClicked(binding.circleImageView8) }
        binding.circleImageView9.setOnClickListener { onCircleImageViewClicked(binding.circleImageView9) }
        binding.circleImageView10.setOnClickListener { onCircleImageViewClicked(binding.circleImageView10) }
        binding.circleImageView11.setOnClickListener { onCircleImageViewClicked(binding.circleImageView11) }
        binding.circleImageView12.setOnClickListener { onCircleImageViewClicked(binding.circleImageView12) }

        binding.saveAvatarButton.setOnClickListener {
            binding.saveAvatarButton.isSelected = true

            if (selectedDefaultAvatar != null) {

                // Get the Bitmap from the drawable resource
                val bitmap = BitmapFactory.decodeResource(resources, selectedDefaultAvatar!!)

                // Set the 'newAvatarBitmap' variable with the loaded Bitmap
                newAvatarBitmap = bitmap

                // save the selected image to database
                saveAvatarToDatabase()
            }
        }

        // custom up button
        binding.expandedPopBack.setOnClickListener {
            binding.imageExpandLayout.visibility = View.GONE
            isImageExpandLayoutVisible = false

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
                        compressedBitmap?.let { newAvatarBitmap = it }

                        // save the selected image to database
                        saveAvatarToDatabase()
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

    private fun saveAvatarToDatabase() {

        binding.apply {

            // display progress bar while waiting to complete upload
            progressBar.visibility = View.VISIBLE
            overlayView.visibility = View.VISIBLE

            // close the cropping layer and expanded image layout
            imageCropLayout.visibility = View.GONE
            imageExpandLayout.visibility = View.GONE
            isImageExpandLayoutVisible = false

            sharedViewModel.changeUserAvatar(
                newAvatarBitmap,
                requireContext()
            ) { isSuccessful ->
                if (isSuccessful) {
                    Toast.makeText(
                        context,
                        "Profile image changed Successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Image upload failed!\n check your network connection",
                        Toast.LENGTH_SHORT
                    )
                        .show()

                    // Stop the loading animation
                    overlayView.visibility = View.GONE
                    progressBar.visibility = View.GONE
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

        // Save the state of imageExpandLayout visibility, changesMade and
        outState.putBoolean("imageExpandLayoutVisible", isImageExpandLayoutVisible)
        outState.putBoolean("changesMade", changesMade)
        outState.putBoolean("userDetailsSetFromViewModel", userDetailsSetFromViewModel)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Restore the state of isImageExpandLayoutVisible and changesMade
        isImageExpandLayoutVisible =
            savedInstanceState?.getBoolean("imageExpandLayoutVisible") ?: false
        changesMade = savedInstanceState?.getBoolean("changesMade") ?: false
        userDetailsSetFromViewModel =
            savedInstanceState?.getBoolean("userDetailsSetFromViewModel") ?: true

        // Reset imageExpand layout visibility
        if (isImageExpandLayoutVisible) {
            binding.imageExpandLayout.visibility = View.VISIBLE
        }
    }

    private fun onCircleImageViewClicked(circleImageView: CircleImageView) {
        // Deselect the previously selected item (if any)
        selectedImageView?.isSelected = false
        selectedImageView?.borderColor = Color.TRANSPARENT // Clear previous border

        // Select the clicked item
        circleImageView.isSelected = true
        val borderColor = ContextCompat.getColor(requireContext(), R.color.myPrimary)
        circleImageView.borderColor = borderColor
        circleImageView.borderWidth = borderWidth // Set border width

        // Select the clicked item
        circleImageView.isSelected = true

        // Update the selectedImageView variable
        selectedImageView = circleImageView

        // Now you can access the selected item's drawable resource ID
        val selectedDrawableResource = when (circleImageView) {

            // Add cases for each CircleImageView item
            binding.circleImageView1 -> R.drawable.asset_1
            binding.circleImageView2 -> R.drawable.asset_2
            binding.circleImageView3 -> R.drawable.asset_3
            binding.circleImageView4 -> R.drawable.asset_4
            binding.circleImageView5 -> R.drawable.asset_5
            binding.circleImageView6 -> R.drawable.asset_6
            binding.circleImageView7 -> R.drawable.asset_7
            binding.circleImageView8 -> R.drawable.asset_8
            binding.circleImageView9 -> R.drawable.asset_9
            binding.circleImageView10 -> R.drawable.asset_10
            binding.circleImageView11 -> R.drawable.asset_11
            binding.circleImageView12 -> R.drawable.asset_12

            else -> null
        }
        selectedDefaultAvatar = selectedDrawableResource
    }
}