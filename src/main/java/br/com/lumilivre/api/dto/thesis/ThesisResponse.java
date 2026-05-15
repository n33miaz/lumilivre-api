package br.com.lumilivre.api.dto.thesis;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThesisResponse {

    private UUID id;
    private String title;
    private String authors;
    private String advisors;
    private String courseName;
    private String completionYear;
    private String completionSemester;
    private String pdfUrl;
    private String coverUrl;
    private String externalUrl;
    private Boolean active;
}
