package br.com.lumilivre.api.service.infra.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Provider para dev/teste: grava blobs em {@code ${lumilivre.storage.local.base-dir}}
 * (default: {@code ./storage}) e devolve uma URL servida por
 * {@code ${lumilivre.storage.local.public-url}} (default: {@code http://localhost:8080/storage}).
 *
 * Ativado por {@code lumilivre.storage.provider=local}. Útil quando o contribuidor
 * não quer subir MinIO/Supabase no docker-compose. Não substitui um provider
 * S3-compatible em produção.
 */
@Component
@ConditionalOnProperty(name = "lumilivre.storage.provider", havingValue = "local")
public class LocalFilesystemStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalFilesystemStorageProvider.class);

    @Value("${lumilivre.storage.local.base-dir:./storage}")
    private String baseDir;

    @Value("${lumilivre.storage.local.public-url:http://localhost:8080/storage}")
    private String publicUrl;

    @Override
    public String name() {
        return "local";
    }

    @Override
    public String upload(MultipartFile file, StorageBucket bucket) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("The file is empty.");
        }
        try {
            Path bucketDir = Paths.get(baseDir, bucket.folder());
            Files.createDirectories(bucketDir);
            String original = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
            String objectName = UUID.randomUUID() + "_" + original.replaceAll("[^A-Za-z0-9._-]", "_");
            Path target = bucketDir.resolve(objectName);
            file.transferTo(target.toFile());
            String url = publicUrl + "/" + bucket.folder() + "/" + objectName;
            log.debug("Local storage upload bucket={} path={} url={}", bucket, target, url);
            return url;
        } catch (IOException e) {
            throw new StorageException("Local filesystem upload failed: " + e.getMessage(), e);
        }
    }
}
