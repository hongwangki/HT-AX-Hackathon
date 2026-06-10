package haitai.ht_ax_hackathon.dto;

import org.springframework.core.io.Resource;

public record AttachmentDownload(Resource resource, String originalFileName, String contentType) {
}
