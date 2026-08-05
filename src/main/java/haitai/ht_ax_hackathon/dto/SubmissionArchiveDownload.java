package haitai.ht_ax_hackathon.dto;

import java.util.List;

public record SubmissionArchiveDownload(List<AttachmentDownload> files, String archiveFileName) {
}
