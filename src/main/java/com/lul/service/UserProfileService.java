package com.lul.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lul.repository.UserProfileRepository;
import com.lul.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;



import java.util.HashMap;
import java.util.Map;

import com.lul.entity.UserProfile;
import com.lul.exception.NotFoundException;
import com.lul.constant.ErrorCode;

@Service
@Slf4j
public class UserProfileService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    public Map<String, Object> getUserProfile(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
            
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("userId", profile.getUser().getUserWorkId());
        profileData.put("username", profile.getUser().getUsername());
        profileData.put("email", profile.getUser().getEmail());
        profileData.put("phoneNumber", profile.getUser().getPhoneNumber());
        profileData.put("firstName", profile.getFirstName());
        profileData.put("lastName", profile.getLastName());
        profileData.put("country", profile.getCountry());
        profileData.put("state", profile.getState());
        profileData.put("city", profile.getCity());
        profileData.put("whatsappNumber", profile.getWhatsappNumber());
        profileData.put("gender", profile.getGender());
        profileData.put("dateOfBirth", profile.getDateOfBirth());
        
        return profileData;
    }
} 