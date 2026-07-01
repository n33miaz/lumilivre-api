package br.com.lumilivre.api.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseStatisticsResponse {

    private String courseName;
    private long readerCount;
    private long totalLoans;

    public double getAvgLoansPerReader() {
        return readerCount == 0 ? 0.0 : (double) totalLoans / readerCount;
    }
}
