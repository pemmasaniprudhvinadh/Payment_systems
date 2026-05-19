package com.example.auth.controller;

import com.example.auth.service.UserService;
import com.example.auth.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        // NOTE: password hash placeholder - replace with real hash
        User user = userService.createUser(username, "HASHED(" + password + ")");
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<?> listUsers() { return ResponseEntity.ok(userService.listUsers()); }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        return userService.getUser(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            User updated = userService.updateUser(id, body.get("username"), "HASHED(" + body.get("password") + ")");
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
