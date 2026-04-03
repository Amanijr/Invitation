package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserResponseDto;
import com.InvitationSystem.InvitationSystem.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
@Tag(name = "Users", description = "User management endpoints")
public class  UserController {

    @Autowired
    private UserService userService;

//    @PostMapping("/register")
//    public ResponseEntity<UserResponseDto> registerUser(@RequestBody UserRequestDto request) {
//        UserResponseDto response = userService.createUser(request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<?> loginUser(@RequestParam String email, @RequestParam String password) {
//        Optional<UserResponseDto> user = userService.authenticate(email, password);
//        if (user.isPresent()) {
//            return ResponseEntity.ok(user.get());
//        } else {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
//        }
//    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID userId) {
        UserResponseDto response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(user))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable UUID userId, @RequestBody UserRequestDto request) {
        UserResponseDto response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
