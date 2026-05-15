package br.com.lumilivre.api.dto.thesis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThesisRequest {

    private String title;
    private String authors;
    private String advisors;
    private Integer courseId;
    private String completionYear;
    private String completionSemester;
    private String externalUrl;

    @Builder.Default
    private Boolean active = true;
}
