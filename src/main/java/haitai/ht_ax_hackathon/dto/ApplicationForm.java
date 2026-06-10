package haitai.ht_ax_hackathon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ApplicationForm {

    @NotBlank(message = "팀명을 입력해 주세요.")
    private String teamName;

    @NotBlank(message = "대표 전화번호를 입력해 주세요.")
    @Pattern(regexp = "^[0-9\\-]{9,20}$", message = "전화번호는 숫자와 하이픈(-)만 사용해 9자 이상 입력해 주세요.")
    private String representativePhone;

    // Blank-allowed here because the admin edit screen reuses this form without a password;
    // the public apply flow enforces presence in the controller.
    private String password;

    @NotEmpty(message = "구성인원을 1명 이상 입력해 주세요.")
    @Valid
    private List<TeamMemberForm> members = new ArrayList<>();

    @NotBlank(message = "주제를 입력해 주세요.")
    private String topic;

    @NotBlank(message = "내용을 입력해 주세요.")
    private String content;

    private List<MultipartFile> attachments = new ArrayList<>();

    public ApplicationForm() {
        members.add(new TeamMemberForm());
    }
}
