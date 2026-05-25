package br.com.lumilivre.api.service.infra.storage;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * Implementação default do {@link StorageProvider}: Supabase Storage via REST.
 * Ativada quando {@code lumilivre.storage.provider=supabase} (default).
 * Mantém o circuit breaker {@code supabaseStorage} já configurado em
 * {@code application.properties}.
 */
@Component
@ConditionalOnProperty(name = "lumilivre.storage.provider", havingValue = "supabase", matchIfMissing = true)
public class SupabaseStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageProvider.class);

    private final Map<StorageBucket, String> bucketNames = new EnumMap<>(StorageBucket.class);

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role.key:${supabase.key}}")
    private String supabaseKey;

    @Value("${supabase.bucket.capas:covers}")
    private String bucketCapas;

    @Value("${supabase.bucket.tccs:theses}")
    private String bucketTccs;

    @Value("${supabase.bucket.avatars:avatars}")
    private String bucketAvatars;

    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public String name() {
        return "supabase";
    }

    @Override
    @CircuitBreaker(name = "supabaseStorage", fallbackMethod = "uploadFallback")
    public String upload(MultipartFile file, StorageBucket bucket) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("The file is empty.");
        }
        validateContentType(file, bucket);

        String bucketName = bucketName(bucket);
        String folderName = bucket.folder();
        String originalFileName = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String objectName = UUID.randomUUID() + "_" + originalFileName;
        String objectPath = folderName + "/" + objectName;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl + "/storage/v1/object/" + bucketName + "/" + objectPath))
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", file.getContentType())
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String encoded = folderName + "/" + URLEncoder.encode(objectName, StandardCharsets.UTF_8)
                        .replace("+", "%20");
                return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + encoded;
            }
            throw new StorageException("Supabase upload failed: " + response.statusCode() + " - " + response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageException("Supabase upload error: " + e.getMessage(), e);
        }
    }

    private void validateContentType(MultipartFile file, StorageBucket bucket) {
        String contentType = file.getContentType();
        switch (bucket) {
            case COVERS, AVATARS -> {
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new StorageException("Only image files are allowed for covers and avatars.");
                }
            }
            case THESES -> {
                if (!"application/pdf".equalsIgnoreCase(contentType)) {
                    throw new StorageException("Only PDF files are allowed for theses.");
                }
            }
        }
    }

    private String bucketName(StorageBucket bucket) {
        if (bucketNames.isEmpty()) {
            bucketNames.put(StorageBucket.COVERS, bucketCapas);
            bucketNames.put(StorageBucket.THESES, bucketTccs);
            bucketNames.put(StorageBucket.AVATARS, bucketAvatars);
        }
        return bucketNames.get(bucket);
    }

    @SuppressWarnings("unused")
    private String uploadFallback(MultipartFile file, StorageBucket bucket, Throwable t) {
        log.error("Supabase Storage unavailable (circuit open) bucket={}: {}", bucket, t.getMessage());
        throw new StorageException("Storage service is temporarily unavailable. Try again.");
    }
}
