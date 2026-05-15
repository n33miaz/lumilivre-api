package br.com.lumilivre.api.service.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role.key:${supabase.key}}")
    private String supabaseKey;

    @Value("${supabase.bucket.capas}")
    private String bucketCapas;

    @Value("${supabase.bucket.tccs}")
    private String bucketTccs;

    @Value("${supabase.bucket.avatars}")
    private String bucketAvatars;

    private final HttpClient client = HttpClient.newHttpClient();

    @CircuitBreaker(name = "supabaseStorage", fallbackMethod = "uploadFileFallback")
    public String uploadFile(MultipartFile file, String tipo) throws IOException, InterruptedException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("The file is empty.");
        }

        String normalizedType = tipo == null ? "" : tipo.toLowerCase(Locale.ROOT);
        String bucketName;
        String folderName;
        boolean imageUpload = false;

        switch (normalizedType) {
            case "capas", "covers" -> {
                bucketName = bucketCapas;
                folderName = "covers";
                imageUpload = true;
            }
            case "tccs", "theses" -> {
                bucketName = bucketTccs;
                folderName = "theses";
                if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
                    throw new IllegalArgumentException("Only PDF files are allowed for theses.");
                }
            }
            case "avatars" -> {
                bucketName = bucketAvatars;
                folderName = "avatars";
                imageUpload = true;
            }
            default -> throw new IllegalArgumentException("Invalid bucket type. Use 'covers', 'theses' or 'avatars'.");
        }

        if (imageUpload && (file.getContentType() == null || !file.getContentType().startsWith("image/"))) {
            throw new IllegalArgumentException("Only image files are allowed for covers and avatars.");
        }

        String originalFileName = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String objectName = UUID.randomUUID() + "_" + originalFileName;
        String fileName = folderName + "/" + objectName;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName))
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer " + supabaseKey)
                .header("Content-Type", file.getContentType())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            String encodedFileName = folderName + "/" + URLEncoder.encode(objectName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + encodedFileName;
        }

        throw new RuntimeException("File upload failed: " + response.statusCode() + " - " + response.body());
    }

    @SuppressWarnings("unused")
    private String uploadFileFallback(MultipartFile file, String tipo, Exception e) {
        log.error("Supabase Storage unavailable (circuit open): {}", e.getMessage());
        throw new RuntimeException("Storage service is temporarily unavailable. Try again.");
    }
}
