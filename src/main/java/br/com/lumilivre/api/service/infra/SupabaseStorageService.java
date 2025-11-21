package br.com.lumilivre.api.service.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket.capas}")
    private String bucketCapas;

    @Value("${supabase.bucket.tccs}")
    private String bucketTccs;

    private final HttpClient client = HttpClient.newHttpClient();

    public String uploadFile(MultipartFile file, String tipo) throws IOException, InterruptedException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo está vazio.");
        }

        String bucketName;
        if ("capas".equalsIgnoreCase(tipo)) {
            bucketName = bucketCapas;
        } else if ("tccs".equalsIgnoreCase(tipo)) {
            bucketName = bucketTccs;
            if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
                throw new IllegalArgumentException("Apenas arquivos PDF são permitidos no bucket de TCCs.");
            }
        } else {
            throw new IllegalArgumentException("Tipo de bucket inválido. Use 'capas' ou 'tccs'.");
        }

        String fileName = tipo.toLowerCase() + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName))
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer " + supabaseKey)
                .header("Content-Type", file.getContentType())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + encodedFileName;
        } else {
            throw new RuntimeException("Erro ao enviar arquivo: " + response.statusCode() + " - " + response.body());
        }
    }
}
