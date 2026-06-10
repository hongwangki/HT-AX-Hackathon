package haitai.ht_ax_hackathon.exception;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(Long id) {
        super("신청서를 찾을 수 없습니다. ID: " + id);
    }
}
