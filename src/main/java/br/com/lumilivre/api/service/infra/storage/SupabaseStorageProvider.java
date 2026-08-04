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

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new StorageException("Could not read the uploaded file.", e);
        }
        // Valida pelo conteudo real (magic bytes), NAO pelo Content-Type
        // do cliente, e devolve um tipo seguro fixo. SVG/HTML sao rejeitados
        // (evita stored XSS servido pela origem do storage).
        String safeContentType = validateAndDetect(bytes, bucket);

        String bucketName = bucketName(bucket);
        String folderName = bucket.folder();
        String originalFileName = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String safeFileName = originalFileName.replaceAll("[^A-Za-z0-9._-]", "_");
        String objectName = UUID.randomUUID() + "_" + safeFileName;
        String objectPath = folderName + "/" + objectName;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl + "/storage/v1/object/" + bucketName + "/" + objectPath))
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", safeContentType)
                    .header("X-Content-Type-Options", "nosniff")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
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

    /**
     * Valida o arquivo pelo seu conteudo real (magic bytes) contra uma
     * allowlist por bucket e devolve o Content-Type seguro a ser gravado.
     * Imagens: apenas raster (PNG/JPEG/WEBP/GIF) — SVG e explicitamente recusado
     * pois pode carregar JavaScript (stored XSS). Documentos: apenas PDF.
     */
    private String validateAndDetect(byte[] bytes, StorageBucket bucket) {
        String sniffed = sniffContentType(bytes);
        switch (bucket) {
            case COVERS, AVATARS -> {
                if (sniffed == null || !sniffed.startsWith("image/")) {
                    throw new StorageException(
                            "Only raster image files (PNG, JPEG, WEBP, GIF) are allowed. SVG is not accepted.");
                }
                return sniffed;
            }
            case THESES -> {
                if (!"application/pdf".equals(sniffed)) {
                    throw new StorageException("Only PDF files are allowed for documents.");
                }
                return sniffed;
            }
            default -> throw new StorageException("Unsupported storage bucket.");
        }
    }

    /** Detecta o tipo por assinatura de bytes; null se nao reconhecido/permitido. */
    private String sniffContentType(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 6 && b[0] == 0x47 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x38
                && (b[4] == 0x37 || b[4] == 0x39) && b[5] == 0x61) {
            return "image/gif";
        }
        if (b.length >= 12 && b[0] == 0x52 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x46
                && b[8] == 0x57 && b[9] == 0x45 && b[10] == 0x42 && b[11] == 0x50) {
            return "image/webp";
        }
        if (b.length >= 5 && b[0] == 0x25 && b[1] == 0x50 && b[2] == 0x44 && b[3] == 0x46 && b[4] == 0x2D) {
            return "application/pdf";
        }
        return null;
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
