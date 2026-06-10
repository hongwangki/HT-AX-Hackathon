package haitai.ht_ax_hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordLookupForm {

    @NotBlank(message = "대표 전화번호를 입력해 주세요.")
    private String representativePhone;
}
