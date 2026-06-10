package haitai.ht_ax_hackathon.exception;

public class AttachmentFileNotFoundException extends RuntimeException {

    public AttachmentFileNotFoundException(Long fileId) {
        super("첨부파일을 찾을 수 없습니다. ID: " + fileId);
    }

    public AttachmentFileNotFoundException(String message) {
        super(message);
    }
}
