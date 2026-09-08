package com.skillswap.service;

import com.skillswap.dto.RegisterRequest;
import com.skillswap.entity.User;
import com.skillswap.enums.Role;
import com.skillswap.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User saveUser(RegisterRequest request) {

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        user.setRole(Role.USER);

        return userRepository.save(user);
    }
}
