package com.example.auth.service;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User createUser(String username, String passwordHash) {
        String id = UUID.randomUUID().toString();
        User user = new User(id, username, passwordHash);
        return repository.save(user);
    }

    public List<User> listUsers() { return repository.findAll(); }

    public Optional<User> getUser(String id) { return repository.findById(id); }

    public User updateUser(String id, String username, String passwordHash) {
        return repository.findById(id).map(u -> {
            u.setUsername(username);
            u.setPasswordHash(passwordHash);
            return repository.save(u);
        }).orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public void deleteUser(String id) { repository.deleteById(id); }
}
