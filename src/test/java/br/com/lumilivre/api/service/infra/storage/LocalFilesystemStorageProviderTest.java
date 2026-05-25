package br.com.lumilivre.api.service.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class LocalFilesystemStorageProviderTest {

    @TempDir
    Path tempDir;

    private LocalFilesystemStorageProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalFilesystemStorageProvider();
        ReflectionTestUtils.setField(provider, "baseDir", tempDir.toString());
        ReflectionTestUtils.setField(provider, "publicUrl", "http://localhost:8080/storage");
    }

    @Test
    void uploadWritesFileToBucketFolderAndReturnsPublicUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "capa.png", "image/png", "fake-bytes".getBytes());

        String url = provider.upload(file, StorageBucket.COVERS);

        assertThat(url).startsWith("http://localhost:8080/storage/covers/");
        assertThat(url).endsWith("_capa.png");

        // Verifica arquivo no disco
        Path bucketDir = tempDir.resolve("covers");
        assertThat(Files.list(bucketDir).count()).isEqualTo(1);
    }

    @Test
    void uploadRejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> provider.upload(empty, StorageBucket.AVATARS))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void uploadSanitizesObjectName() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "arquivo com espaço & símbolos.pdf", "application/pdf", "x".getBytes());

        String url = provider.upload(file, StorageBucket.THESES);

        assertThat(url).contains("/theses/");
        assertThat(url).doesNotContain(" ");
        assertThat(url).doesNotContain("&");
    }

    @Test
    void nameIsLocal() {
        assertThat(provider.name()).isEqualTo("local");
    }
}
