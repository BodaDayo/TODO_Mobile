package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.databinding.FragmentProfileBinding
import com.rgbstudios.todomobile.ui.adapters.AvatarAdapter
import com.rgbstudios.todomobile.utils.AvatarManager
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.utils.ToastManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import com.rgbstudios.todomobile.viewmodel.TodoViewModelFactory
import java.io.ByteArrayOutputStream
import java.io.File

class ProfileFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels {
        TodoViewModelFactory(activity?.application as TodoMobileApplication)
    }

    private lateinit var binding: FragmentProfileBinding
    private lateinit var newAvatarBitmap: Bitmap
    private var changesMade = false
    private var isImageSampleLayoutVisible = false
    private var isExpandedImageLayoutVisible = false
    private var userDetailsSetFromViewModel = false
    private var selectedDefaultAvatar: Bitmap? = null
    private var defaultChanges = false
    private var avatarHolder: Bitmap? = null
    private lateinit var fragmentContext: Context
    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private val avatarManager = AvatarManager()
    private lateinit var avatarAdapter: AvatarAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        fragmentContext = requireContext()
        changesMade = savedInstanceState?.getBoolean("changesMade") ?: false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val defaultAvatarList = avatarManager.defaultAvatarsList

        binding.apply {

            // Set up the adapter
            avatarAdapter = AvatarAdapter(defaultAvatarList) { avatar ->

                selectedDefaultAvatar = BitmapFactory.decodeResource(resources, avatar)

                defaultChanges = true

                imageSampleView.setImageBitmap(selectedDefaultAvatar)
                expandedImageView.setImageBitmap(selectedDefaultAvatar)

            }

            // Set up the recyclerview with the adapter
            defaultAvatarRecyclerView.setHasFixedSize(true)
            defaultAvatarRecyclerView.layoutManager = GridLayoutManager(context, 4)
            defaultAvatarRecyclerView.adapter = avatarAdapter

            // Set up listeners
            profileImageView.setOnClickListener {
                // keep a reference of the current newAvatar
                avatarHolder = newAvatarBitmap

                // Set the preview imageViews
                imageSampleView.setImageBitmap(avatarHolder)
                expandedImageView.setImageBitmap(avatarHolder)

                // Show the imageSample layout
                imageSampleLayout.visibility = View.VISIBLE

                // Pick image from local storage
                editAvatar.setOnClickListener {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }

                // Custom up button
                imageSamplePopBack.setOnClickListener {

                    // Reset the preview imageViews
                    imageSampleView.setImageBitmap(avatarHolder)
                    expandedImageView.setImageBitmap(avatarHolder)

                    imageSampleLayout.visibility = View.GONE
                    isImageSampleLayoutVisible = false
                }

                // Save avatar button
                saveAvatarButton.setOnClickListener {
                    saveAvatarButton.isSelected = true

                    if (defaultChanges) {

                        // Set the 'newAvatarBitmap' variable with the currentAvatar Bitmap
                        if (selectedDefaultAvatar != null) {
                            newAvatarBitmap = selectedDefaultAvatar!!
                        }

                        defaultChanges = false

                        // save the selected image to database
                        saveAvatarToDatabase()
                    }
                }

                // Expand the avatar
                imageSampleView.setOnClickListener {
                    expandedImageLayout.visibility = View.VISIBLE

                    // close expanded image layout
                    closeExpandedImage.setOnClickListener {
                        expandedImageLayout.visibility = View.GONE

                        isExpandedImageLayoutVisible = false
                    }

                    isExpandedImageLayoutVisible = true
                }

                isImageSampleLayoutVisible = true
            }

            changeAvatar.setOnClickListener {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            saveButton.setOnClickListener {

                val name = nameEditText.text.toString()
                val occupation = occupationText.text.toString()

                if (changesMade) {
                    sharedViewModel.updateUserDetails(name, occupation) { isSuccessful ->
                        if (isSuccessful) {
                            toastManager.showShortToast(
                                fragmentContext,
                                "Changes saved Successfully!"
                            )

                            activity?.supportFragmentManager?.popBackStack()
                        } else {
                            toastManager.showLongToast(
                                fragmentContext,
                                "Something went wrong...\nTry Again!"
                            )
                        }
                    }
                } else {
                    activity?.supportFragmentManager?.popBackStack()
                }
            }

            // Set up text listeners
            nameEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    changesMade = userDetailsSetFromViewModel
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            occupationText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!userDetailsSetFromViewModel) {
                        userDetailsSetFromViewModel = true
                        changesMade = false
                    } else {
                        changesMade = true
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            // Custom up button
            popBack.setOnClickListener {
                popBackStackManager()
            }

            // Customize onBackPressed method
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                if (isExpandedImageLayoutVisible) {

                    expandedImageLayout.visibility = View.GONE
                    isExpandedImageLayoutVisible = false
                } else if (isImageSampleLayoutVisible) {

                    imageSampleLayout.visibility = View.GONE
                    isImageSampleLayoutVisible = false
                } else {
                    popBackStackManager()
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
                                        .into(profileImageView)

                                    // stop loading animation
                                    overlayView.visibility = View.GONE
                                    progressBar.visibility = View.GONE
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    // Do nothing
                                }
                            })
                    } else {

                        // Stop the loading animation
                        overlayView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                    }

                    // Set user email
                    emailText.text = user.email

                    // Set user details

                    // Set the name EditText
                    if (!user.name.isNullOrEmpty()) nameEditText.text =
                        Editable.Factory.getInstance().newEditable(user.name)
                    // Set the occupation EditText
                    if (!user.occupation.isNullOrEmpty()) occupationText.text =
                        Editable.Factory.getInstance().newEditable(user.occupation)
                }
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
            imageSampleLayout.visibility = View.GONE
            isImageSampleLayoutVisible = false

            sharedViewModel.changeUserAvatar(
                newAvatarBitmap,
                requireContext()
            ) { isSuccessful ->
                if (isSuccessful) {
                    toastManager.showShortToast(
                        fragmentContext,
                        "Profile image changed Successfully!"
                    )
                } else {
                    toastManager.showShortToast(
                        fragmentContext,
                        "Image upload failed!\n check your network connection"
                    )
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
        if (changesMade) {
            dialogManager.showDiscardDialog(this) { isSuccessful ->
                if (isSuccessful) {
                    // Reset changesMade flag
                    changesMade = false

                    // pop back stack
                    activity?.supportFragmentManager?.popBackStack()
                }
            }
        } else {
            // If no changes, simply pop the back stack
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the state of flags
        outState.putBoolean("imageSampleLayoutVisible", isImageSampleLayoutVisible)
        outState.putBoolean("isExpandedImageLayoutVisible", isExpandedImageLayoutVisible)
        outState.putBoolean("changesMade", changesMade)
        outState.putBoolean("userDetailsSetFromViewModel", userDetailsSetFromViewModel)
        outState.putBoolean("defaultChanges", defaultChanges)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Restore the state of flags
        isImageSampleLayoutVisible =
            savedInstanceState?.getBoolean("imageSampleLayoutVisible") ?: false
        isExpandedImageLayoutVisible =
            savedInstanceState?.getBoolean("isExpandedImageLayoutVisible") ?: false
        changesMade =
            savedInstanceState?.getBoolean("changesMade") ?: false
        userDetailsSetFromViewModel =
            savedInstanceState?.getBoolean("userDetailsSetFromViewModel") ?: false
        defaultChanges =
            savedInstanceState?.getBoolean("defaultChanges", defaultChanges) ?: false

        binding.apply {
            // Reset imageSample layout visibility
            if (isImageSampleLayoutVisible) {
                imageSampleLayout.visibility = View.VISIBLE
            }
            // Reset imageExpand layout visibility
            if (isExpandedImageLayoutVisible) {
                expandedImageLayout.visibility = View.VISIBLE
            }
        }
    }

}