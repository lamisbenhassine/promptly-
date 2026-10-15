package com.promptly.backend.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.course.dto.CourseRequest;
import com.promptly.backend.course.dto.CourseResponse;
import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;
import com.promptly.backend.user.UserService;

/**
 * Regles metier des cours.
 *
 * La visibilite et le droit de modification sont verifies ici, et non dans le
 * controleur : un appelant qui contournerait l'interface obtiendrait le meme
 * refus.
 */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserService userService;

    public CourseService(CourseRepository courseRepository, UserService userService) {
        this.courseRepository = courseRepository;
        this.userService = userService;
    }

    public Page<CourseResponse> search(String viewerEmail,
                                       CourseCategory category,
                                       Boolean published,
                                       Pageable pageable) {
        User viewer = userService.findByEmail(viewerEmail);
        boolean seeAll = viewer.getRole() == Role.ADMIN;

        // Un etudiant ne peut pas contourner le filtre "publie" : le parametre
        // est ignore s'il n'a pas le droit de voir les brouillons.
        Boolean publishedFilter = seeAll ? published : null;

        return courseRepository
                .search(category, publishedFilter, seeAll, viewer, pageable)
                .map(CourseResponse::from);
    }

    public CourseResponse getVisible(Long id, String viewerEmail) {
        User viewer = userService.findByEmail(viewerEmail);
        Course course = findById(id);

        if (!isVisible(course, viewer)) {
            // 404 plutot que 403 : un etudiant ne doit pas pouvoir inferer
            // l'existence d'un brouillon en enumerant les identifiants.
            throw new ResourceNotFoundException("Cours", id);
        }
        return CourseResponse.from(course);
    }

    @Transactional
    public CourseResponse create(String authorEmail, CourseRequest request) {
        User author = userService.findByEmail(authorEmail);
        ensureCanAuthor(author);

        User instructor = resolveInstructor(author, request.instructorId());
        Course course = new Course(
                request.title().trim(),
                request.description().trim(),
                request.learningObjectives().trim(),
                request.category(),
                request.published(),
                instructor);

        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse update(Long id, String editorEmail, CourseRequest request) {
        User editor = userService.findByEmail(editorEmail);
        Course course = findById(id);
        ensureCanManage(course, editor);

        course.setTitle(request.title().trim());
        course.setDescription(request.description().trim());
        course.setLearningObjectives(request.learningObjectives().trim());
        course.setCategory(request.category());
        course.setPublished(request.published());
        course.setInstructor(resolveInstructor(editor, request.instructorId()));

        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public void delete(Long id, String editorEmail) {
        User editor = userService.findByEmail(editorEmail);
        Course course = findById(id);
        ensureCanManage(course, editor);
        courseRepository.delete(course);
    }

    private Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cours", id));
    }

    /**
     * Un brouillon n'est visible que par son enseignant et par un administrateur.
     */
    private boolean isVisible(Course course, User viewer) {
        if (course.isPublished()) {
            return true;
        }
        if (viewer.getRole() == Role.ADMIN) {
            return true;
        }
        return course.getInstructor().getId().equals(viewer.getId());
    }

    private void ensureCanAuthor(User user) {
        if (user.getRole() != Role.ENSEIGNANT && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seuls les enseignants et les administrateurs peuvent creer un cours");
        }
    }

    private void ensureCanManage(Course course, User editor) {
        if (editor.getRole() == Role.ADMIN) {
            return;
        }
        if (editor.getRole() == Role.ENSEIGNANT
                && course.getInstructor().getId().equals(editor.getId())) {
            return;
        }
        throw new AccessDeniedException("Vous ne pouvez modifier que vos propres cours");
    }

    /**
     * Un enseignant est toujours rattache a lui-meme. Un administrateur peut
     * designer un autre enseignant ou administrateur, jamais un etudiant.
     */
    private User resolveInstructor(User actor, Long requestedInstructorId) {
        if (actor.getRole() != Role.ADMIN || requestedInstructorId == null) {
            return actor;
        }

        User instructor = userService.findById(requestedInstructorId);
        if (instructor.getRole() != Role.ENSEIGNANT && instructor.getRole() != Role.ADMIN) {
            throw new BusinessRuleException(
                    "L'enseignant d'un cours doit avoir le role ENSEIGNANT ou ADMIN");
        }
        return instructor;
    }
}
