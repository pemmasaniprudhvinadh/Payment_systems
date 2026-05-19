package com.example.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        // TODO: implement real authentication (DB, password hashing)
        // For now return a fake JWT placeholder. Replace with real JWT generation.
        String token = "eyJhbGciOi...FAKE_TOKEN_REPLACE";
        return ResponseEntity.ok(Map.of("access_token", token));
    }
}
