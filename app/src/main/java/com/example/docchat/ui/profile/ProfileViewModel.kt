package com.example.docchat.ui.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.docchat.ui.UserProfile

class ProfileViewModel : ViewModel() {

    private val profileRepository = ProfileRepository()
    private val _profileData = MutableLiveData<UserProfile?>()
    val profileData: MutableLiveData<UserProfile?> get() = _profileData

    fun fetchUserProfile() {
        profileRepository.getUserProfile { profile ->
            _profileData.postValue(profile)
        }
    }
}
