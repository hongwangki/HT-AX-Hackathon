package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.domain.AttachmentFile;
import haitai.ht_ax_hackathon.config.FileUploadProperties;
import haitai.ht_ax_hackathon.exception.FileStorageException;
import haitai.ht_ax_hackathon.exception.AttachmentFileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path uploadDirectory;

    public FileStorageService(FileUploadProperties properties) {
        this.uploadDirectory = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
    }

    /** Saves a browser upload with a UUID name and returns its database metadata. */
    public AttachmentFile store(MultipartFile multipartFile) {
        String originalFileName = getSafeOriginalFileName(multipartFile);
        String storedFileName = UUID.randomUUID() + getExtension(originalFileName);
        Path destination = uploadDirectory.resolve(storedFileName).normalize();

        try {
            Files.createDirectories(uploadDirectory);
            multipartFile.transferTo(destination);
            return new AttachmentFile(
                    originalFileName,
                    storedFileName,
                    destination.toString(),
                    multipartFile.getSize(),
                    multipartFile.getContentType()
            );
        } catch (IOException exception) {
            deleteQuietly(destination.toString());
            throw new FileStorageException("첨부파일 저장에 실패했습니다: " + originalFileName, exception);
        }
    }

    /** Physical file cleanup must not prevent application database deletion. */
    public void deleteQuietly(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (Exception exception) {
            log.warn("Failed to delete uploaded file: {}", filePath, exception);
        }
    }

    public Resource loadAsResource(String filePath) {
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists() || !resource.isReadable()) {
            throw new AttachmentFileNotFoundException("첨부파일을 읽을 수 없습니다.");
        }
        return resource;
    }

    private String getSafeOriginalFileName(MultipartFile multipartFile) {
        String originalFileName = multipartFile.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            return "attachment";
        }
        return Paths.get(originalFileName).getFileName().toString();
    }

    private String getExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex >= 0 ? fileName.substring(extensionIndex) : "";
    }
}
