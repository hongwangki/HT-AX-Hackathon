package haitai.ht_ax_hackathon.dto;

public record HtmlPreview(
        Long fileId,
        String displayFileName,
        String previewUrl,
        boolean archive
) {
}
