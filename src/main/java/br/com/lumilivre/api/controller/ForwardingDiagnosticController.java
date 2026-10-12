package br.com.lumilivre.api.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * TEMPORÁRIO — diagnóstico do IP real atrás do proxy do Render.
 *
 * <p>Existe só para descobrir qual cabeçalho a borda do Render preenche com o IP
 * real do cliente, já que o {@code X-Forwarded-For} é forjável. Devolve apenas
 * metadados da própria requisição de quem chama (nenhum segredo, nenhum dado de
 * outro usuário). DEVE ser removido assim que o resolver estiver confirmado.
 */
@RestController
@RequestMapping("/api/_diag")
@RequiredArgsConstructor
public class ForwardingDiagnosticController {

    private final ClientIpResolver clientIpResolver;

    @GetMapping("/forwarding")
    public ResponseEntity<Map<String, String>> forwarding(HttpServletRequest request) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("remoteAddr", String.valueOf(request.getRemoteAddr()));
        out.put("resolved", String.valueOf(clientIpResolver.resolve(request)));
        for (String h : new String[] {
                "X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP", "True-Client-IP",
                "Forwarded", "X-Forwarded-Host", "X-Forwarded-Proto", "Fly-Client-IP",
                "X-Client-IP", "X-Cluster-Client-IP" }) {
            String v = request.getHeader(h);
            if (v != null) {
                out.put("hdr:" + h, v);
            }
        }
        return ResponseEntity.ok(out);
    }
}
