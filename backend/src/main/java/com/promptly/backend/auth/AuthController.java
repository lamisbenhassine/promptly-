package com.promptly.backend.auth;

import java.net.URI;
import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.promptly.backend.auth.dto.AuthResponse;
import com.promptly.backend.auth.dto.LoginRequest;
import com.promptly.backend.auth.dto.RegisterRequest;
import com.promptly.backend.user.UserService;
import com.promptly.backend.user.dto.UserResponse;

import jakarta.validation.Valid;

/**
 * Points d'entree d'authentification.
 *
 * Les deux premieres routes sont publiques, la troisieme exige un jeton valide.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * Inscription. Renvoie 201 avec l'en-tete Location, conformement aux
     * conventions REST pour une creation de ressource.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    /** Connexion. Renvoie le jeton JWT et le profil de l'utilisateur. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Profil de l'utilisateur connecte.
     *
     * {@code Principal} est fourni par Spring Security et contient l'adresse
     * electronique extraite du jeton. L'identite n'est donc jamais lue depuis
     * le corps de la requete, ce qui empeche un utilisateur de consulter le
     * profil d'un autre en modifiant les parametres envoyes.
     */
    @GetMapping("/me")
    public UserResponse currentUser(Principal principal) {
        return UserResponse.from(userService.findByEmail(principal.getName()));
    }
}
