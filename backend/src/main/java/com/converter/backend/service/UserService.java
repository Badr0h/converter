package com.converter.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.converter.backend.dto.user.UserCreateDto;
import com.converter.backend.dto.user.UserResponseDto;
import com.converter.backend.dto.user.UserUpdateDto;
import com.converter.backend.exception.EmailAlreadyExistsException;
import com.converter.backend.exception.ResourceNotFoundException;
import com.converter.backend.model.User;
import com.converter.backend.repository.UserRepository;


import jakarta.transaction.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;


@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,PasswordEncoder  passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponseDto> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    public UserResponseDto getUserById(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User not found with id : " + id));

        return mapToResponseDto(user);
    }

    public UserResponseDto getUserByEmail(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("User not found with email: " + email));
        return mapToResponseDto(user);
    }

    public UserResponseDto updateUser(Long id, UserUpdateDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Vérifier si l’email est déjà utilisé par un autre utilisateur
        if (userRepository.existsByEmail(dto.getEmail()) && !user.getEmail().equals(dto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use: " + dto.getEmail());
        }

        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User updatedUser = userRepository.save(user);

        return mapToResponseDto(updatedUser);
    }
    

    public UserResponseDto updateUserRole(Long id, String roleName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        try {
            User.Role role = User.Role.valueOf(roleName.toUpperCase());
            user.setRole(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleName);
        }
        return mapToResponseDto(userRepository.save(user));
    }

    public UserResponseDto toggleUserEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEnabled(!user.getEnabled());
        return mapToResponseDto(userRepository.save(user));
    }

    public void deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }



    
    public UserResponseDto adminCreateUser(UserCreateDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setRole(dto.getRole() != null ? User.Role.valueOf(dto.getRole()) : User.Role.USER);
        user.setEnabled(true); // Admin-created users are enabled by default
        return mapToResponseDto(userRepository.save(user));
    }

    private UserResponseDto mapToResponseDto(User user){
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setEnabled(user.getEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto ;
    }


    

  

}
