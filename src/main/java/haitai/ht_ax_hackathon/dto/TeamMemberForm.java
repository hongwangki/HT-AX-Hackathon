package haitai.ht_ax_hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamMemberForm {

    @NotBlank(message = "소속을 입력해 주세요.")
    private String department;

    @NotBlank(message = "사번을 입력해 주세요.")
    @Pattern(regexp = "\\d{7}", message = "사번은 숫자 7자리로 입력해 주세요.")
    private String employeeNo;

    @NotBlank(message = "이름을 입력해 주세요.")
    private String name;
}
