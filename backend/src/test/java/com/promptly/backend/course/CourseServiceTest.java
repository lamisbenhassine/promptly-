package com.promptly.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.course.dto.CourseRequest;
import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;
import com.promptly.backend.user.UserService;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CourseService courseService;

    private User teacher;
    private User otherTeacher;
    private User student;
    private User admin;

    @BeforeEach
    void setUp() {
        teacher = user(1L, "a@promptly.local", Role.ENSEIGNANT);
        otherTeacher = user(2L, "b@promptly.local", Role.ENSEIGNANT);
        student = user(3L, "c@promptly.local", Role.ETUDIANT);
        admin = user(4L, "admin@promptly.local", Role.ADMIN);
    }

    @Nested
    @DisplayName("Visibilite")
    class Visibility {

        @Test
        @DisplayName("Un etudiant n'obtient pas un brouillon (404, pas 403)")
        void studentCannotSeeDraft() {
            Course draft = course(10L, teacher, false);
            given(userService.findByEmail(student.getEmail())).willReturn(student);
            given(courseRepository.findById(10L)).willReturn(Optional.of(draft));

            assertThatThrownBy(() -> courseService.getVisible(10L, student.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("L'enseignant proprietaire voit son brouillon")
        void ownerSeesOwnDraft() {
            Course draft = course(10L, teacher, false);
            given(userService.findByEmail(teacher.getEmail())).willReturn(teacher);
            given(courseRepository.findById(10L)).willReturn(Optional.of(draft));

            assertThat(courseService.getVisible(10L, teacher.getEmail()).id()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("Un etudiant ne peut pas creer de cours")
        void studentCannotCreate() {
            given(userService.findByEmail(student.getEmail())).willReturn(student);

            assertThatThrownBy(() -> courseService.create(student.getEmail(), request(null)))
                    .isInstanceOf(AccessDeniedException.class);
            verify(courseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Un enseignant est rattache a lui-meme, meme s'il transmet un autre identifiant")
        void teacherCannotAssignSomeoneElse() {
            given(userService.findByEmail(teacher.getEmail())).willReturn(teacher);
            given(courseRepository.save(any(Course.class))).willAnswer(i -> i.getArgument(0));

            courseService.create(teacher.getEmail(), request(otherTeacher.getId()));

            ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
            verify(courseRepository).save(captor.capture());
            assertThat(captor.getValue().getInstructor()).isEqualTo(teacher);
            verify(userService, never()).findById(any());
        }

        @Test
        @DisplayName("Un administrateur ne peut pas designer un etudiant comme enseignant")
        void adminCannotAssignStudent() {
            given(userService.findByEmail(admin.getEmail())).willReturn(admin);
            given(userService.findById(student.getId())).willReturn(student);

            assertThatThrownBy(() -> courseService.create(admin.getEmail(), request(student.getId())))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ENSEIGNANT ou ADMIN");
        }
    }

    @Nested
    @DisplayName("Modification")
    class Update {

        @Test
        @DisplayName("Un enseignant ne peut pas modifier le cours d'un collegue")
        void teacherCannotEditColleague() {
            Course course = course(10L, otherTeacher, true);
            given(userService.findByEmail(teacher.getEmail())).willReturn(teacher);
            given(courseRepository.findById(10L)).willReturn(Optional.of(course));

            assertThatThrownBy(() -> courseService.update(10L, teacher.getEmail(), request(null)))
                    .isInstanceOf(AccessDeniedException.class);
            verify(courseRepository, never()).save(any());
        }
    }

    private static CourseRequest request(Long instructorId) {
        return new CourseRequest(
                "Introduction a Java",
                "Les bases du langage Java pour debuter.",
                "Ecrire une classe simple et compiler un programme.",
                CourseCategory.JAVA,
                true,
                instructorId);
    }

    private static User user(Long id, String email, Role role) {
        User user = new User("Prenom", "Nom", email, "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Course course(Long id, User instructor, boolean published) {
        Course course = new Course(
                "Titre",
                "Description suffisamment longue.",
                "Objectifs suffisamment longs.",
                CourseCategory.JAVA,
                published,
                instructor);
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }
}
