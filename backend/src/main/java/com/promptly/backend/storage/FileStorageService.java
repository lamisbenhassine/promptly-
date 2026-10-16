package com.promptly.backend.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;

import jakarta.annotation.PostConstruct;

/**
 * Stockage local des fichiers (images et PDF).
 *
 * Les binaires ne sont pas places en base MySQL : seuls les chemins relatifs
 * sont enregistres. Cela simplifie les sauvegardes et evite de gonfler la base.
 *
 * Les noms de fichiers portent un UUID pour empecher les collisions et rendre
 * les URL difficilement devinables.
 */
@Service
public class FileStorageService {

    public static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    public static final Set<String> PDF_TYPES = Set.of("application/pdf");

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PDF_BYTES = 20L * 1024 * 1024;

    private final Path root;

    public FileStorageService(@Value("${promptly.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
    }

    /**
     * Enregistre un fichier et renvoie le chemin relatif utilise ensuite dans
     * l'URL publique {@code /api/files/...}.
     */
    public String store(MultipartFile file, String folder, Set<String> allowedTypes, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Aucun fichier n'a ete envoye");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessRuleException(
                    "Fichier trop volumineux (maximum " + (maxBytes / (1024 * 1024)) + " Mo)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BusinessRuleException("Type de fichier non autorise : " + contentType);
        }

        String extension = extensionFor(contentType, file.getOriginalFilename());
        String relative = folder + "/" + UUID.randomUUID() + extension;

        try {
            Path destination = root.resolve(relative).normalize();
            if (!destination.startsWith(root)) {
                throw new BusinessRuleException("Chemin de stockage invalide");
            }
            Files.createDirectories(destination.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return relative.replace('\\', '/');
        } catch (IOException e) {
            throw new BusinessRuleException("Echec de l'enregistrement du fichier");
        }
    }

    public String storeImage(MultipartFile file, String folder) {
        return store(file, folder, IMAGE_TYPES, MAX_IMAGE_BYTES);
    }

    public String storePdf(MultipartFile file, String folder) {
        return store(file, folder, PDF_TYPES, MAX_PDF_BYTES);
    }

    public Resource loadAsResource(String relativePath) {
        try {
            Path file = resolveExisting(relativePath);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Fichier", relativePath);
            }
            return resource;
        } catch (IOException e) {
            throw new ResourceNotFoundException("Fichier", relativePath);
        }
    }

    /** Chemin absolu d'un fichier deja stocke (pour lecture locale, ex. PDFBox). */
    public Path resolveExisting(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ResourceNotFoundException("Fichier", relativePath);
        }
        Path file = root.resolve(relativePath).normalize();
        if (!file.startsWith(root) || !Files.exists(file)) {
            throw new ResourceNotFoundException("Fichier", relativePath);
        }
        return file;
    }

    public void deleteQuietly(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path file = root.resolve(relativePath).normalize();
            if (file.startsWith(root)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ignored) {
            // La suppression du fichier n'est pas critique pour la reponse API.
        }
    }

    private String extensionFor(String contentType, String originalName) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "application/pdf" -> ".pdf";
            default -> {
                if (originalName != null && originalName.contains(".")) {
                    yield originalName.substring(originalName.lastIndexOf('.'));
                }
                yield "";
            }
        };
    }
}
