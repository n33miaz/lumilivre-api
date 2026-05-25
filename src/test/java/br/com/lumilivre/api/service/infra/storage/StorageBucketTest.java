package br.com.lumilivre.api.service.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StorageBucketTest {

    @Test
    void fromLegacyAcceptsPortugueseAndEnglishAliases() {
        assertThat(StorageBucket.fromLegacy("capas")).isEqualTo(StorageBucket.COVERS);
        assertThat(StorageBucket.fromLegacy("covers")).isEqualTo(StorageBucket.COVERS);
        assertThat(StorageBucket.fromLegacy("tccs")).isEqualTo(StorageBucket.THESES);
        assertThat(StorageBucket.fromLegacy("theses")).isEqualTo(StorageBucket.THESES);
        assertThat(StorageBucket.fromLegacy("avatars")).isEqualTo(StorageBucket.AVATARS);
        assertThat(StorageBucket.fromLegacy("COVERS")).isEqualTo(StorageBucket.COVERS);
    }

    @Test
    void fromLegacyRejectsUnknownValues() {
        assertThatThrownBy(() -> StorageBucket.fromLegacy("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid");
        assertThatThrownBy(() -> StorageBucket.fromLegacy(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void folderReturnsCanonicalName() {
        assertThat(StorageBucket.COVERS.folder()).isEqualTo("covers");
        assertThat(StorageBucket.THESES.folder()).isEqualTo("theses");
        assertThat(StorageBucket.AVATARS.folder()).isEqualTo("avatars");
    }
}
