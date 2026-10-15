package com.promptly.backend.user;

import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.promptly.backend.user.dto.ChangePasswordRequest;
import com.promptly.backend.user.dto.UpdateProfileRequest;
import com.promptly.backend.user.dto.UpdateRoleRequest;
import com.promptly.backend.user.dto.UpdateStatusRequest;
import com.promptly.backend.user.dto.UserResponse;

import jakarta.validation.Valid;

/**
 * Gestion des utilisateurs.
 *
 * Deux familles de routes cohabitent :
 * <ul>
 *   <li>{@code /me...} : l'utilisateur agit sur son propre compte ;</li>
 *   <li>{@code /{id}...} : un administrateur agit sur le compte d'un autre.</li>
 * </ul>
 *
 * Les secondes sont protegees par {@code @PreAuthorize("hasRole('ADMIN')")}.
 * Placer la restriction directement sur la methode la rend visible a la
 * lecture du code metier, et non uniquement dans un fichier de configuration
 * distant.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Modification de son propre profil. */
    @PutMapping("/me")
    public UserResponse updateMyProfile(Principal principal,
                                        @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getName(), request);
    }

    /** Changement de son propre mot de passe. Renvoie 204 (pas de contenu). */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(Principal principal,
                                                 @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    /** Liste paginee des utilisateurs, triee par date de creation decroissante. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> listUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return userService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(@PathVariable Long id) {
        return UserResponse.from(userService.findById(id));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRole(@PathVariable Long id,
                                   @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateRole(id, request.role());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateStatusRequest request) {
        return userService.updateStatus(id, request.enabled());
    }
}
