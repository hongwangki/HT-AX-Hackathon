package haitai.ht_ax_hackathon.exception;

public class TaskSubmissionSizeExceededException extends RuntimeException {

    public TaskSubmissionSizeExceededException() {
        super("첨부파일 총합은 최대 500MB까지 가능합니다.");
    }
}
