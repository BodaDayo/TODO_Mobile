package com.rgbstudios.todomobile

import android.app.ProgressDialog.show
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.rgbstudios.todomobile.data.Avatars
import com.rgbstudios.todomobile.databinding.FragmentSignUpBinding
import com.rgbstudios.todomobile.model.TaskViewModel
import java.io.ByteArrayOutputStream


class SignUpFragment : Fragment() {

    private val sharedViewModel: TaskViewModel by activityViewModels()

    private lateinit var binding: FragmentSignUpBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        registerEvents()
    }

    private fun registerEvents() {

        binding.signInTv.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }
        binding.SignUpButton.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passEt.text.toString().trim()
            val confirmPass = binding.confirmPassEt.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {

                    binding.progressBar.visibility = View.VISIBLE
                    auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                        if (it.isSuccessful) {
                            // set userEmail
                            setCurrentUserEmail(email)

                            // Store the user data in the Firebase database
                            assignUserAvatar()

                            Toast.makeText(
                                context,
                                "Registered Successfully!, Let's get things done! \uD83D\uDE80",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
                        } else {
                            val errorMessage =
                                it.exception?.message?.substringAfter(": ")
                                    ?: "Unknown error occurred!\nTry Again"
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                        binding.progressBar.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Passwords do not match. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(context, "Empty fields are not allowed !!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setCurrentUserEmail(email: String) {
        sharedViewModel.saveUserEmailReferenceOnAuth(email)
    }

    private fun assignUserAvatar() {
        val currentUser: FirebaseUser? = auth.currentUser
        val userId: String? = currentUser?.uid

        if (userId != null) {
            val storageReference = FirebaseStorage.getInstance().reference.child("avatars").child(userId)

            // Get the drawable resource ID of a random avatar
            val avatars = Avatars()
            val avatarResource = avatars.getAvatar()

            // Convert the drawable resource to a bitmap
            val avatarBitmap = BitmapFactory.decodeResource(resources, avatarResource)

            sharedViewModel.uploadUserAvatar(avatarBitmap) { isSuccessful ->
                if (isSuccessful) {
                    // Handle Success
                } else {
                    // Handle failure
                }
            }

            // Convert the bitmap to a ByteArrayOutputStream
            val outputStream = ByteArrayOutputStream()
            avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val avatarData = outputStream.toByteArray()

            // Upload the avatar image to Firebase Storage
            val uploadTask = storageReference.putBytes(avatarData)
            uploadTask.addOnSuccessListener {
                // Image upload successful, you can show a toast or perform other actions
            }.addOnFailureListener {
                // Handle image upload failure

                // Toast.makeText(context, "Unable to Set Avatar!", Toast.LENGTH_SHORT).show()
            }
        }
    }


}