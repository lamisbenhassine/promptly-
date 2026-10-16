package com.promptly.backend.chapter;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.promptly.backend.chapter.dto.ChapterRequest;
import com.promptly.backend.chapter.dto.ChapterResponse;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.course.Course;
import com.promptly.backend.course.CourseService;
import com.promptly.backend.storage.FileStorageService;

@Service
@Transactional(readOnly = true)
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseService courseService;
    private final FileStorageService fileStorageService;

    public ChapterService(ChapterRepository chapterRepository,
                          CourseService courseService,
                          FileStorageService fileStorageService) {
        this.chapterRepository = chapterRepository;
        this.courseService = courseService;
        this.fileStorageService = fileStorageService;
    }

    public List<ChapterResponse> listByCourse(Long courseId, String viewerEmail) {
        courseService.requireVisible(courseId, viewerEmail);
        return chapterRepository.findByCourseIdOrderByPositionAsc(courseId).stream()
                .map(ChapterResponse::from)
                .toList();
    }

    public ChapterResponse get(Long courseId, Long chapterId, String viewerEmail) {
        courseService.requireVisible(courseId, viewerEmail);
        return ChapterResponse.from(findInCourse(courseId, chapterId));
    }

    @Transactional
    public ChapterResponse create(Long courseId, String editorEmail, ChapterRequest request) {
        Course course = courseService.requireManageable(courseId, editorEmail);

        int position = request.position() != null
                ? request.position()
                : chapterRepository.findMaxPosition(courseId) + 1;

        Chapter chapter = new Chapter(
                request.title().trim(),
                request.content().trim(),
                blankToNull(request.codeExample()),
                position,
                course);

        return ChapterResponse.from(chapterRepository.save(chapter));
    }

    @Transactional
    public ChapterResponse update(Long courseId, Long chapterId, String editorEmail,
                                  ChapterRequest request) {
        courseService.requireManageable(courseId, editorEmail);
        Chapter chapter = findInCourse(courseId, chapterId);

        chapter.setTitle(request.title().trim());
        chapter.setContent(request.content().trim());
        chapter.setCodeExample(blankToNull(request.codeExample()));
        if (request.position() != null) {
            chapter.setPosition(request.position());
        }

        return ChapterResponse.from(chapterRepository.save(chapter));
    }

    @Transactional
    public ChapterResponse uploadPdf(Long courseId, Long chapterId, String editorEmail,
                                     MultipartFile file) {
        courseService.requireManageable(courseId, editorEmail);
        Chapter chapter = findInCourse(courseId, chapterId);

        fileStorageService.deleteQuietly(chapter.getPdfPath());
        chapter.setPdfPath(fileStorageService.storePdf(file, "chapter-pdfs"));
        return ChapterResponse.from(chapterRepository.save(chapter));
    }

    @Transactional
    public void delete(Long courseId, Long chapterId, String editorEmail) {
        courseService.requireManageable(courseId, editorEmail);
        Chapter chapter = findInCourse(courseId, chapterId);
        fileStorageService.deleteQuietly(chapter.getPdfPath());
        chapterRepository.delete(chapter);
    }

    private Chapter findInCourse(Long courseId, Long chapterId) {
        return chapterRepository.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapitre", chapterId));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
