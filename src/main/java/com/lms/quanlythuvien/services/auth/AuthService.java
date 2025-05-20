package com.lms.quanlythuvien.services.auth;

import com.lms.quanlythuvien.models.user.User;
import com.lms.quanlythuvien.services.user.UserService;
import com.lms.quanlythuvien.utils.security.PasswordUtils;

import java.util.Optional;

public class AuthService {

    private final UserService userService; // SỬ DỤNG UserService

    public AuthService(UserService userService) { // NHẬN UserService qua constructor
        System.out.println("DEBUG_AS_CONSTRUCTOR: AuthService constructor started (with UserService).");
        this.userService = userService;
        // Không cần initializeAdminAccount() ở đây nữa, UserService đã làm
        System.out.println("DEBUG_AS_CONSTRUCTOR: AuthService constructor finished.");
    }

    public AuthResult login(String email, String password) {
        System.out.println("DEBUG_AS_LOGIN: AuthService.login() - Method Started. Attempting login for Email: [" + email + "], Password length: [" + (password != null ? password.length() : "null") + "]");

        Optional<User> userOptional = userService.findUserByEmail(email); // GỌI TỪ UserService

        if (userOptional.isEmpty()) {
            System.out.println("DEBUG_AS_LOGIN: User not found for email: [" + email + "] via UserService.");
            return AuthResult.failure("Incorrect email or password. Please try again.");
        }

        User user = userOptional.get();
        System.out.println("DEBUG_AS_LOGIN: User found via UserService: [" + user.getEmail() + "]. Stored password hash: [" + user.getPasswordHash() + "]");

        if (password == null || password.isEmpty()) {
            System.out.println("DEBUG_AS_LOGIN: Input password is null or empty. Verification will fail.");
            return AuthResult.failure("Password cannot be empty.");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            System.err.println("CRITICAL_AS_LOGIN: Stored password hash for user [" + user.getEmail() + "] is null or empty! Cannot verify.");
            return AuthResult.failure("User account configuration error. Please contact support.");
        }

        System.out.println("DEBUG_AS_LOGIN: Verifying password: [length " + password.length() + "] against hash: [" + user.getPasswordHash() + "]");
        try {
            boolean passwordsMatch = PasswordUtils.verifyPassword(password, user.getPasswordHash());
            System.out.println("DEBUG_AS_LOGIN: PasswordUtils.verifyPassword result for [" + user.getEmail() + "]: " + passwordsMatch);

            if (passwordsMatch) {
                System.out.println("DEBUG_AS_LOGIN: Password verification successful for " + user.getEmail());
                return AuthResult.success(user);
            } else {
                System.out.println("DEBUG_AS_LOGIN: Password verification FAILED for " + user.getEmail());
                return AuthResult.failure("Incorrect email or password. Please try again.");
            }
        } catch (Exception e) {
            System.err.println("CRITICAL_AS_LOGIN: Exception during password verification for " + user.getEmail());
            e.printStackTrace();
            return AuthResult.failure("An internal error occurred during authentication. Please contact support.");
        }
    }

    public AuthResult register(String username, String email, String rawPassword, User.Role role) {
        System.out.println("DEBUG_AS_REGISTER: Attempting to register user. Email: [" + email + "], Username: [" + username + "]");

        if (userService.isUsernameTaken(username)) { // GỌI TỪ UserService
            System.out.println("DEBUG_AS_REGISTER: Username [" + username + "] already taken (checked via UserService).");
            return AuthResult.failure("Username already exists. Please choose another one.");
        }
        if (userService.isEmailTaken(email)) { // GỌI TỪ UserService
            System.out.println("DEBUG_AS_REGISTER: Email [" + email + "] already taken (checked via UserService).");
            return AuthResult.failure("Email already registered. Please use a different email or login.");
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            System.out.println("DEBUG_AS_REGISTER: Registration failed - Raw password is empty.");
            return AuthResult.failure("Password cannot be empty for registration.");
        }

        String hashedPassword = null;
        try {
            hashedPassword = PasswordUtils.hashPassword(rawPassword);
            System.out.println("DEBUG_AS_REGISTER: Password hashed for new user [" + email + "].");
        } catch (Exception e) {
            System.err.println("CRITICAL_AS_REGISTER: Exception while hashing password for new user [" + email + "]");
            e.printStackTrace();
            return AuthResult.failure("Error processing password during registration.");
        }

        if (hashedPassword == null || hashedPassword.isEmpty()){
            System.err.println("CRITICAL_AS_REGISTER: Hashed password is null or empty for new user [" + email + "]");
            return AuthResult.failure("Failed to secure password during registration.");
        }

        User newUser = new User(username, email, hashedPassword, role);

        boolean added = userService.addUser(newUser); // GỌI TỪ UserService
        if (added) {
            System.out.println("DEBUG_AS_REGISTER: New user registered successfully via UserService: " + newUser.getEmail() + " with role " + role);
            return AuthResult.success(newUser);
        } else {
            System.err.println("ERROR_AS_REGISTER: Failed to add user via UserService for email: " + email);
            return AuthResult.failure("Could not register user due to an internal issue or data conflict.");
        }
    }
}