package haitai.ht_ax_hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TaskSubmissionForm {

    @NotBlank(message = "대표 전화번호가 비어 있습니다. 신청현황에서 다시 들어와 주세요.")
    private String representativePhone;

    @NotBlank(message = "비밀번호가 비어 있습니다. 신청현황에서 다시 들어와 주세요.")
    private String password;

    @Size(max = 500, message = "한 줄 설명은 500자 이내로 입력해 주세요.")
    private String summary;

    @Size(max = 500, message = "URL은 500자 이내로 입력해 주세요.")
    private String demoUrl;

    private List<MultipartFile> attachments = new ArrayList<>();
}
