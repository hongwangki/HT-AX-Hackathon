package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.exception.ApplicationNotFoundException;
import haitai.ht_ax_hackathon.exception.AttachmentFileNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ApplicationNotFoundException.class, AttachmentFileNotFoundException.class})
    public String handleNotFound(RuntimeException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error/404";
    }

    /** Oversized or broken uploads get a guided message instead of the whitelabel page. */
    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public String handleUploadTooLarge(Exception exception, Model model) {
        model.addAttribute("message",
                "첨부파일 용량이 허용 범위를 초과했습니다. 파일당 100MB, 전체 200MB 이하로 올려 주세요.");
        return "error";
    }
}
