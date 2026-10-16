package com.promptly.backend.forum;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.forum.dto.ForumCommentRequest;
import com.promptly.backend.forum.dto.ForumCommentResponse;
import com.promptly.backend.forum.dto.ForumPostRequest;
import com.promptly.backend.forum.dto.ForumPostResponse;
import com.promptly.backend.forum.dto.ReactionSummary;
import com.promptly.backend.storage.FileStorageService;
import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;
import com.promptly.backend.user.UserService;

@Service
@Transactional(readOnly = true)
public class ForumService {

    private final ForumPostRepository postRepository;
    private final ForumCommentRepository commentRepository;
    private final ForumReactionRepository reactionRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public ForumService(ForumPostRepository postRepository,
                        ForumCommentRepository commentRepository,
                        ForumReactionRepository reactionRepository,
                        UserService userService,
                        FileStorageService fileStorageService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    public Page<ForumPostResponse> list(String viewerEmail, ForumCategory category,
                                        Pageable pageable) {
        User viewer = userService.findByEmail(viewerEmail);
        return postRepository.search(category, pageable)
                .map(post -> ForumPostResponse.summary(
                        post,
                        commentRepository.countByPostId(post.getId()),
                        buildReactions(post.getId(), viewer.getId())));
    }

    public ForumPostResponse get(Long id, String viewerEmail) {
        User viewer = userService.findByEmail(viewerEmail);
        ForumPost post = findPost(id);
        List<ForumCommentResponse> comments = commentRepository
                .findByPostIdOrderByCreatedAtAsc(id).stream()
                .map(ForumCommentResponse::from)
                .toList();
        return ForumPostResponse.detail(post, comments, buildReactions(id, viewer.getId()));
    }

    @Transactional
    public ForumPostResponse createPost(String authorEmail, ForumPostRequest request) {
        User author = userService.findByEmail(authorEmail);
        ForumPost post = new ForumPost(
                request.title().trim(),
                request.content().trim(),
                request.category(),
                author);
        ForumPost saved = postRepository.save(post);
        return ForumPostResponse.detail(saved, List.of(), ReactionSummary.empty());
    }

    @Transactional
    public ForumPostResponse updatePost(Long id, String editorEmail, ForumPostRequest request) {
        User editor = userService.findByEmail(editorEmail);
        ForumPost post = findPost(id);
        ensureCanEditPost(post, editor);

        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setCategory(request.category());
        return get(postRepository.save(post).getId(), editorEmail);
    }

    @Transactional
    public ForumPostResponse uploadImage(Long id, String editorEmail, MultipartFile file) {
        User editor = userService.findByEmail(editorEmail);
        ForumPost post = findPost(id);
        ensureCanEditPost(post, editor);

        fileStorageService.deleteQuietly(post.getImagePath());
        String path = fileStorageService.storeImage(file, "forum-images");
        post.setImagePath(path);
        return get(postRepository.save(post).getId(), editorEmail);
    }

    @Transactional
    public void deletePost(Long id, String editorEmail) {
        User editor = userService.findByEmail(editorEmail);
        ForumPost post = findPost(id);
        ensureCanModerateOrOwn(post.getAuthor().getId(), editor);
        reactionRepository.deleteByPostId(id);
        commentRepository.deleteByPostId(id);
        fileStorageService.deleteQuietly(post.getImagePath());
        postRepository.delete(post);
    }

    @Transactional
    public ForumPostResponse setLocked(Long id, String moderatorEmail, boolean locked) {
        User moderator = userService.findByEmail(moderatorEmail);
        ensureModerator(moderator);
        ForumPost post = findPost(id);
        post.setLocked(locked);
        return get(postRepository.save(post).getId(), moderatorEmail);
    }

    @Transactional
    public ForumPostResponse react(Long postId, String userEmail, ReactionType type) {
        User user = userService.findByEmail(userEmail);
        ForumPost post = findPost(postId);

        ForumReaction reaction = reactionRepository
                .findByPostIdAndUserId(postId, user.getId())
                .orElse(null);

        if (reaction == null) {
            reactionRepository.save(new ForumReaction(post, user, type));
        } else if (reaction.getType() == type) {
            // Clic sur la meme reaction = retrait (comme Facebook).
            reactionRepository.delete(reaction);
        } else {
            reaction.setType(type);
            reactionRepository.save(reaction);
        }

        return get(postId, userEmail);
    }

    @Transactional
    public ForumPostResponse removeReaction(Long postId, String userEmail) {
        User user = userService.findByEmail(userEmail);
        findPost(postId);
        reactionRepository.findByPostIdAndUserId(postId, user.getId())
                .ifPresent(reactionRepository::delete);
        return get(postId, userEmail);
    }

    @Transactional
    public ForumCommentResponse addComment(Long postId, String authorEmail,
                                           ForumCommentRequest request) {
        User author = userService.findByEmail(authorEmail);
        ForumPost post = findPost(postId);
        if (post.isLocked()) {
            throw new BusinessRuleException("Ce sujet est verrouille : aucun commentaire n'est accepte.");
        }

        ForumComment comment = new ForumComment(request.content().trim(), post, author);
        ForumComment saved = commentRepository.save(comment);
        post.touch();
        postRepository.save(post);
        return ForumCommentResponse.from(saved);
    }

    @Transactional
    public ForumCommentResponse updateComment(Long postId, Long commentId, String editorEmail,
                                              ForumCommentRequest request) {
        User editor = userService.findByEmail(editorEmail);
        ForumComment comment = findComment(postId, commentId);
        ensureCanEditComment(comment, editor);
        comment.setContent(request.content().trim());
        return ForumCommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, String editorEmail) {
        User editor = userService.findByEmail(editorEmail);
        ForumComment comment = findComment(postId, commentId);
        ensureCanModerateOrOwn(comment.getAuthor().getId(), editor);
        commentRepository.delete(comment);
    }

    private ReactionSummary buildReactions(Long postId, Long viewerId) {
        List<Object[]> rows = reactionRepository.countByTypeForPost(postId);
        ReactionType mine = reactionRepository.findByPostIdAndUserId(postId, viewerId)
                .map(ForumReaction::getType)
                .orElse(null);
        return ReactionSummary.from(rows, mine);
    }

    private ForumPost findPost(Long id) {
        return postRepository.findWithAuthorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publication", id));
    }

    private ForumComment findComment(Long postId, Long commentId) {
        return commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire", commentId));
    }

    private void ensureCanEditPost(ForumPost post, User editor) {
        if (isModerator(editor) || post.getAuthor().getId().equals(editor.getId())) {
            return;
        }
        throw new AccessDeniedException("Vous ne pouvez pas modifier cette publication.");
    }

    private void ensureCanEditComment(ForumComment comment, User editor) {
        if (isModerator(editor) || comment.getAuthor().getId().equals(editor.getId())) {
            return;
        }
        throw new AccessDeniedException("Vous ne pouvez pas modifier ce commentaire.");
    }

    private void ensureCanModerateOrOwn(Long authorId, User editor) {
        if (isModerator(editor) || authorId.equals(editor.getId())) {
            return;
        }
        throw new AccessDeniedException("Action reservee a l'auteur ou a un moderateur.");
    }

    private void ensureModerator(User user) {
        if (!isModerator(user)) {
            throw new AccessDeniedException("Action reservee aux enseignants et administrateurs.");
        }
    }

    private boolean isModerator(User user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.ENSEIGNANT;
    }
}
