package haitai.ht_ax_hackathon.dto;

public record HtmlPreviewContent(
        byte[] content,
        String fileName,
        String contentType
) {
}
