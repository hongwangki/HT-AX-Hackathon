package haitai.ht_ax_hackathon.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file")
public class FileUploadProperties {

    private String uploadDir = "uploads";
}
