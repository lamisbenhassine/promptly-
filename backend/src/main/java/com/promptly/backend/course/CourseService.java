package com.promptly.backend.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.promptly.backend.chapter.Chapter;
import com.promptly.backend.chapter.ChapterRepository;
import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.course.dto.CourseRequest;
import com.promptly.backend.course.dto.CourseResponse;
import com.promptly.backend.storage.FileStorageService;
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
    private final ChapterRepository chapterRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public CourseService(CourseRepository courseRepository,
                         ChapterRepository chapterRepository,
                         UserService userService,
                         FileStorageService fileStorageService) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    public Page<CourseResponse> search(String viewerEmail,
                                       CourseCategory category,
                                       Boolean published,
                                       Pageable pageable) {
        User viewer = userService.findByEmail(viewerEmail);
        boolean seeAll = viewer.getRole() == Role.ADMIN;

        Boolean publishedFilter = seeAll ? published : null;

        return courseRepository
                .search(category, publishedFilter, seeAll, viewer, pageable)
                .map(CourseResponse::from);
    }

    public CourseResponse getVisible(Long id, String viewerEmail) {
        return CourseResponse.from(requireVisible(id, viewerEmail));
    }

    /**
     * Charge un cours visible pour l'utilisateur (utilise aussi par les chapitres).
     */
    public Course requireVisible(Long id, String viewerEmail) {
        User viewer = userService.findByEmail(viewerEmail);
        Course course = findById(id);
        if (!isVisible(course, viewer)) {
            throw new ResourceNotFoundException("Cours", id);
        }
        return course;
    }

    /**
     * Charge un cours que l'utilisateur a le droit de modifier.
     */
    public Course requireManageable(Long id, String editorEmail) {
        User editor = userService.findByEmail(editorEmail);
        Course course = findById(id);
        ensureCanManage(course, editor);
        return course;
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
        Course course = requireManageable(id, editorEmail);
        User editor = userService.findByEmail(editorEmail);

        course.setTitle(request.title().trim());
        course.setDescription(request.description().trim());
        course.setLearningObjectives(request.learningObjectives().trim());
        course.setCategory(request.category());
        course.setPublished(request.published());
        course.setInstructor(resolveInstructor(editor, request.instructorId()));

        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse uploadImage(Long id, String editorEmail, MultipartFile file) {
        Course course = requireManageable(id, editorEmail);
        fileStorageService.deleteQuietly(course.getImagePath());
        String path = fileStorageService.storeImage(file, "course-images");
        course.setImagePath(path);
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse uploadPdf(Long id, String editorEmail, MultipartFile file) {
        Course course = requireManageable(id, editorEmail);
        fileStorageService.deleteQuietly(course.getPdfPath());
        String path = fileStorageService.storePdf(file, "course-pdfs");
        course.setPdfPath(path);
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public void delete(Long id, String editorEmail) {
        Course course = requireManageable(id, editorEmail);

        for (Chapter chapter : chapterRepository.findByCourseIdOrderByPositionAsc(id)) {
            fileStorageService.deleteQuietly(chapter.getPdfPath());
        }
        chapterRepository.deleteByCourseId(id);

        fileStorageService.deleteQuietly(course.getImagePath());
        fileStorageService.deleteQuietly(course.getPdfPath());
        courseRepository.delete(course);
    }

    private Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cours", id));
    }

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
