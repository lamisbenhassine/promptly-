package com.promptly.backend.auth;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.promptly.backend.auth.GoogleTokenService.GoogleProfile;
import com.promptly.backend.auth.dto.AuthResponse;
import com.promptly.backend.auth.dto.GoogleLoginRequest;
import com.promptly.backend.auth.dto.LoginRequest;
import com.promptly.backend.auth.dto.RegisterRequest;
import com.promptly.backend.common.exception.EmailAlreadyUsedException;
import com.promptly.backend.security.JwtService;
import com.promptly.backend.user.AuthProvider;
import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;
import com.promptly.backend.user.UserRepository;
import com.promptly.backend.user.dto.UserResponse;

/** Inscription et connexion des utilisateurs. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final GoogleTokenService googleTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       GoogleTokenService googleTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.googleTokenService = googleTokenService;
    }

    /**
     * Cree un compte avec le role ETUDIANT.
     *
     * Le role n'est jamais lu depuis la requete : le forcer cote serveur est la
     * seule garantie qu'un utilisateur ne puisse pas s'inscrire directement
     * comme administrateur en modifiant la requete HTTP.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }

        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.ETUDIANT);

        User saved = userRepository.save(user);
        log.info("Nouveau compte cree : {} (role {})", saved.getEmail(), saved.getRole());
        return UserResponse.from(saved);
    }

    /**
     * Verifie les identifiants et emet un jeton JWT.
     *
     * La verification est deleguee a l'{@code AuthenticationManager} de Spring
     * Security : il compare le mot de passe via BCrypt et leve automatiquement
     * une {@code DisabledException} si le compte est desactive. Ces exceptions
     * sont traduites en reponses HTTP par le gestionnaire global.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        // L'authentification a reussi : l'utilisateur existe necessairement.
        User user = userRepository.findByEmail(email).orElseThrow();

        Instant expiresAt = jwtService.expirationFromNow();
        String token = jwtService.generateToken(user, expiresAt);

        log.info("Connexion reussie : {}", user.getEmail());
        return AuthResponse.of(token, expiresAt, UserResponse.from(user));
    }

    /**
     * Connexion (ou inscription implicite) via Google Sign-In.
     *
     * <ol>
     *   <li>Le jeton Google est verifie cote serveur.</li>
     *   <li>Si un compte existe deja avec cette adresse, on le reutilise.</li>
     *   <li>Sinon un compte ETUDIANT est cree (sans mot de passe utilisable).</li>
     *   <li>Un JWT Promptly est emis, comme pour une connexion classique.</li>
     * </ol>
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleProfile profile = googleTokenService.verify(request.idToken());

        User user = userRepository.findByEmail(profile.email())
                .orElseGet(() -> createGoogleUser(profile));

        if (!user.isEnabled()) {
            throw new DisabledException("Compte desactive");
        }

        Instant expiresAt = jwtService.expirationFromNow();
        String token = jwtService.generateToken(user, expiresAt);

        log.info("Connexion Google reussie : {}", user.getEmail());
        return AuthResponse.of(token, expiresAt, UserResponse.from(user));
    }

    private User createGoogleUser(GoogleProfile profile) {
        String randomPassword = passwordEncoder.encode("google-" + UUID.randomUUID());
        User user = new User(
                profile.firstName(),
                profile.lastName(),
                profile.email(),
                randomPassword,
                Role.ETUDIANT,
                AuthProvider.GOOGLE);
        User saved = userRepository.save(user);
        log.info("Compte Google cree : {}", saved.getEmail());
        return saved;
    }
}
