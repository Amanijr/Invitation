package com.InvitationSystem.InvitationSystem.security;

import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeskUsers {

    private final UserRepository userRepository;

    public User require(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Sign in to continue.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found for " + authentication.getName()));
    }
}
