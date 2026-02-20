package io.github.snguyenanh1.urlshortener.controller;

import io.github.snguyenanh1.urlshortener.dto.user.UserSignIn;
import io.github.snguyenanh1.urlshortener.entity.User;
import io.github.snguyenanh1.urlshortener.security.JwtService;
import jakarta.validation.Valid;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserSignIn signIn) {
        Authentication authRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(signIn.getUsernameOrEmail(), signIn.getPassword());
        Authentication authenticated = authenticationManager.authenticate(authRequest);
        String jwtToken = null;
        if (authenticated.getPrincipal() instanceof User user) {
            jwtToken = jwtService.generateToken(user.getId());
        }
        return ResponseEntity.ok(new JwtResponse(jwtToken));
    }

    @Getter
    private class JwtResponse {
        private final String token;
        private final String type = "Bearer";

        public JwtResponse(String token) {
            this.token = token;
        }

    }
}
