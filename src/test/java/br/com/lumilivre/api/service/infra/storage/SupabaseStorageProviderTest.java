package br.com.lumilivre.api.service.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class SupabaseStorageProviderTest {

    private HttpClient httpClient;
    private SupabaseStorageProvider provider;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        provider = new SupabaseStorageProvider();
        ReflectionTestUtils.setField(provider, "supabaseUrl", "http://supabase.test");
        ReflectionTestUtils.setField(provider, "supabaseKey", "service-role-key");
        ReflectionTestUtils.setField(provider, "bucketCapas", "covers");
        ReflectionTestUtils.setField(provider, "bucketTccs", "theses");
        ReflectionTestUtils.setField(provider, "bucketAvatars", "avatars");
        ReflectionTestUtils.setField(provider, "client", httpClient);
    }

    @Test
    void nameIsSupabase() {
        assertThat(provider.name()).isEqualTo("supabase");
    }

    @Test
    void uploadReturnsPublicUrlOnSuccess() throws Exception {
        MockMultipartFile cover = new MockMultipartFile("file", "capa.png", "image/png", "fake".getBytes());
        stubResponse(200, "");

        String url = provider.upload(cover, StorageBucket.COVERS);

        assertThat(url)
                .startsWith("http://supabase.test/storage/v1/object/public/covers/covers/")
                .endsWith("_capa.png");
    }

    @Test
    void uploadSanitizesUnsafeFilenameCharacters() throws Exception {
        MockMultipartFile cover = new MockMultipartFile(
                "file", "imagem com espaco & simbolos.png", "image/png", "fake".getBytes());
        stubResponse(201, "");

        String url = provider.upload(cover, StorageBucket.COVERS);

        assertThat(url)
                .doesNotContain(" ")
                .doesNotContain("&")
                .contains("imagem_com_espaco___simbolos.png");
    }

    @Test
    void uploadFailsWhenResponseStatusIsNotSuccess() throws Exception {
        MockMultipartFile cover = new MockMultipartFile("file", "capa.png", "image/png", "fake".getBytes());
        stubResponse(500, "internal error");

        assertThatThrownBy(() -> provider.upload(cover, StorageBucket.COVERS))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("500")
                .hasMessageContaining("internal error");
    }

    @Test
    void uploadRejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> provider.upload(empty, StorageBucket.COVERS))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void uploadRejectsNonImageForCoverBucket() {
        MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> provider.upload(pdf, StorageBucket.COVERS))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("image");
    }

    @Test
    void uploadRejectsNonPdfForThesisBucket() {
        MockMultipartFile image = new MockMultipartFile("file", "capa.png", "image/png", "x".getBytes());

        assertThatThrownBy(() -> provider.upload(image, StorageBucket.THESES))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void uploadWrapsIoExceptionAsStorageException() throws Exception {
        MockMultipartFile cover = new MockMultipartFile("file", "capa.png", "image/png", "fake".getBytes());
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("connection reset"));

        assertThatThrownBy(() -> provider.upload(cover, StorageBucket.COVERS))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("connection reset");
    }

    @Test
    void fallbackTranslatesCircuitOpenToStorageException() {
        Throwable cause = new RuntimeException("circuit open");
        MultipartFile file = new MockMultipartFile("file", "x.png", "image/png", "x".getBytes());

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                provider, "uploadFallback", file, StorageBucket.COVERS, cause))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("temporarily unavailable");
    }

    private void stubResponse(int statusCode, String body) throws Exception {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(response);
    }
}
