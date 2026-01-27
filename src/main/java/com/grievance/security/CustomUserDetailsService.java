package com.grievance.security;

import com.grievance.entity.User;
import com.grievance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        User user = userRepository.findByName(name)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with name: " + name));

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("User account is deactivated");
        }

        return user;
    }
}