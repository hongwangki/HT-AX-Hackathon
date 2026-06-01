package haitai.ht_ax_hackathon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
