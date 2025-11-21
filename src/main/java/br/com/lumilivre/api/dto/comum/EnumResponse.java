package br.com.lumilivre.api.dto.comum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnumResponse {
    
    private String nome;
    private String status;
}