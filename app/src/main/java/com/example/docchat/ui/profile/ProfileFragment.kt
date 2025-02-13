package com.example.docchat.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.docchat.R
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var birthdayTextView: TextView
    private lateinit var addressTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        initViews(view)
        setupProfileObserver()
        profileViewModel.fetchUserProfile()

        return view
    }

    private fun initViews(view: View) {
        nameTextView = view.findViewById(R.id.nameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        phoneTextView = view.findViewById(R.id.phoneNumberTextView)
        profileImageView = view.findViewById(R.id.profileImage)
        birthdayTextView = view.findViewById(R.id.birthdayTextView)
        addressTextView = view.findViewById(R.id.addressTextView)
    }

    private fun setupProfileObserver() {
        profileViewModel.profileData.observe(viewLifecycleOwner) { profile ->
            nameTextView.text = profile?.name ?: ""
            emailTextView.text = FirebaseAuth.getInstance().currentUser?.email ?: ""
            phoneTextView.text = profile?.phone ?: ""
            birthdayTextView.text = profile?.birthday ?: ""
            addressTextView.text = profile?.location ?: ""

            Glide.with(this).load(profile?.profileImage ?: "").into(profileImageView)
        }
    }
}
