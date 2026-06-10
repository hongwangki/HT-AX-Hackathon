package haitai.ht_ax_hackathon.controller;

import haitai.ht_ax_hackathon.exception.ApplicationNotFoundException;
import haitai.ht_ax_hackathon.exception.AttachmentFileNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ApplicationNotFoundException.class, AttachmentFileNotFoundException.class})
    public String handleNotFound(RuntimeException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error/404";
    }
}
