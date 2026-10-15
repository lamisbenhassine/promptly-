package com.promptly.backend.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.promptly.backend.user.User;
import com.promptly.backend.user.UserRepository;

/**
 * Pont entre l'entite metier {@link User} et le modele de securite de Spring.
 *
 * Cette classe evite de faire implementer {@code UserDetails} par l'entite :
 * celle-ci reste un objet metier pur, independant du framework de securite.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));

        // Le drapeau "enabled" est transmis a Spring Security : un compte
        // desactive provoque automatiquement une DisabledException a la connexion.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole().asAuthority())))
                .disabled(!user.isEnabled())
                .build();
    }
}
