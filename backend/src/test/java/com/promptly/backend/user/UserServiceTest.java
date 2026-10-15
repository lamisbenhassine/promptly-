package com.promptly.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.user.dto.ChangePasswordRequest;
import com.promptly.backend.user.dto.UpdateProfileRequest;

/**
 * Tests unitaires des regles metier de {@link UserService}.
 *
 * Les dependances sont simulees : ces tests ne necessitent ni base de donnees
 * ni contexte Spring, et s'executent en quelques millisecondes.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User etudiant;

    @BeforeEach
    void setUp() {
        etudiant = new User("Lamis", "Ben Hassine", "lamis@promptly.local",
                "$2a$hache", Role.ETUDIANT);
    }

    @Nested
    @DisplayName("Modification du profil")
    class UpdateProfile {

        @Test
        @DisplayName("Met a jour le nom et le prenom en supprimant les espaces superflus")
        void shouldTrimNames() {
            given(userRepository.findByEmail("lamis@promptly.local"))
                    .willReturn(Optional.of(etudiant));
            given(userRepository.save(any(User.class))).willAnswer(i -> i.getArgument(0));

            var result = userService.updateProfile("lamis@promptly.local",
                    new UpdateProfileRequest("  Lamis  ", "  Ben Hassine  "));

            assertThat(result.firstName()).isEqualTo("Lamis");
            assertThat(result.lastName()).isEqualTo("Ben Hassine");
        }

        @Test
        @DisplayName("Echoue si l'utilisateur n'existe pas")
        void shouldFailWhenUserMissing() {
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfile("inconnu@promptly.local",
                    new UpdateProfileRequest("Jean", "Dupont")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Changement de mot de passe")
    class ChangePassword {

        @Test
        @DisplayName("Refuse le changement si le mot de passe actuel est incorrect")
        void shouldRejectWrongCurrentPassword() {
            given(userRepository.findByEmail("lamis@promptly.local"))
                    .willReturn(Optional.of(etudiant));
            given(passwordEncoder.matches("mauvais", "$2a$hache")).willReturn(false);

            assertThatThrownBy(() -> userService.changePassword("lamis@promptly.local",
                    new ChangePasswordRequest("mauvais", "NouveauPass1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("mot de passe actuel est incorrect");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Refuse un nouveau mot de passe identique a l'actuel")
        void shouldRejectSamePassword() {
            given(userRepository.findByEmail("lamis@promptly.local"))
                    .willReturn(Optional.of(etudiant));
            given(passwordEncoder.matches("ActuelPass1", "$2a$hache")).willReturn(true);

            assertThatThrownBy(() -> userService.changePassword("lamis@promptly.local",
                    new ChangePasswordRequest("ActuelPass1", "ActuelPass1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("different de l'actuel");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Enregistre le nouveau mot de passe hache quand tout est valide")
        void shouldEncodeNewPassword() {
            given(userRepository.findByEmail("lamis@promptly.local"))
                    .willReturn(Optional.of(etudiant));
            given(passwordEncoder.matches("ActuelPass1", "$2a$hache")).willReturn(true);
            given(passwordEncoder.matches("NouveauPass1", "$2a$hache")).willReturn(false);
            given(passwordEncoder.encode("NouveauPass1")).willReturn("$2a$nouveau");

            userService.changePassword("lamis@promptly.local",
                    new ChangePasswordRequest("ActuelPass1", "NouveauPass1"));

            assertThat(etudiant.getPassword()).isEqualTo("$2a$nouveau");
            verify(userRepository).save(etudiant);
        }
    }

    @Nested
    @DisplayName("Protection du dernier administrateur")
    class LastAdminProtection {

        private User admin;

        @BeforeEach
        void setUpAdmin() {
            admin = new User("Admin", "Promptly", "admin@promptly.local",
                    "$2a$hache", Role.ADMIN);
            given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        }

        @Test
        @DisplayName("Refuse de retirer le role ADMIN du dernier administrateur actif")
        void shouldRejectDemotingLastAdmin() {
            given(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).willReturn(1L);

            assertThatThrownBy(() -> userService.updateRole(1L, Role.ETUDIANT))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("dernier administrateur actif");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Refuse de desactiver le dernier administrateur actif")
        void shouldRejectDisablingLastAdmin() {
            given(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).willReturn(1L);

            assertThatThrownBy(() -> userService.updateStatus(1L, false))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("dernier administrateur actif");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Autorise la retrogradation s'il reste un autre administrateur actif")
        void shouldAllowDemotionWhenAnotherAdminExists() {
            given(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).willReturn(2L);
            given(userRepository.save(any(User.class))).willAnswer(i -> i.getArgument(0));

            var result = userService.updateRole(1L, Role.ENSEIGNANT);

            assertThat(result.role()).isEqualTo(Role.ENSEIGNANT);
        }
    }
}
