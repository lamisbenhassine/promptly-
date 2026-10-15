package com.promptly.backend.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.user.dto.ChangePasswordRequest;
import com.promptly.backend.user.dto.UpdateProfileRequest;
import com.promptly.backend.user.dto.UserResponse;

/**
 * Regles metier relatives aux utilisateurs.
 *
 * Toute la logique est placee ici plutot que dans le controleur : celui-ci se
 * limite a traduire HTTP vers Java et inversement. Les regles restent ainsi
 * testables sans serveur web et reutilisables par d'autres appelants.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", email));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
    }

    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Change le mot de passe apres verification de l'ancien.
     *
     * Cette verification est indispensable : sans elle, un jeton vole
     * permettrait de s'approprier definitivement le compte.
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessRuleException("Le mot de passe actuel est incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleException(
                    "Le nouveau mot de passe doit etre different de l'actuel");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /**
     * Modifie le role d'un utilisateur.
     *
     * Regle de protection : on refuse de retirer le role ADMIN s'il s'agit du
     * dernier administrateur actif, sans quoi la plateforme deviendrait
     * impossible a administrer.
     */
    @Transactional
    public UserResponse updateRole(Long id, Role newRole) {
        User user = findById(id);

        if (user.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            ensureNotLastActiveAdmin(user, "retirer le role ADMIN");
        }

        user.setRole(newRole);
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Active ou desactive un compte.
     *
     * Meme protection que ci-dessus : desactiver le dernier administrateur actif
     * reviendrait a se verrouiller hors de la plateforme.
     */
    @Transactional
    public UserResponse updateStatus(Long id, boolean enabled) {
        User user = findById(id);

        if (!enabled && user.getRole() == Role.ADMIN) {
            ensureNotLastActiveAdmin(user, "desactiver ce compte");
        }

        user.setEnabled(enabled);
        return UserResponse.from(userRepository.save(user));
    }

    private void ensureNotLastActiveAdmin(User user, String action) {
        long activeAdmins = userRepository.countByRoleAndEnabledTrue(Role.ADMIN);
        if (activeAdmins <= 1 && user.isEnabled()) {
            throw new BusinessRuleException(
                    "Impossible de " + action + " : il s'agit du dernier administrateur actif");
        }
    }
}
