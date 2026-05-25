package br.com.lumilivre;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "app.scheduling.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:lumilivre_context_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "jwt.secret=test-secret-key-with-enough-length-for-hmac-signature",
        "jwt.expiration=86400000",
        "supabase.url=https://example.supabase.co",
        "supabase.key=test-key",
        "supabase.service-role.key=test-service-role-key",
        "supabase.bucket.capas=capas",
        "supabase.bucket.tccs=tcc",
        "supabase.bucket.avatars=avatars",
        "supabase.storage.base-url-capas=https://example.supabase.co/storage/v1/object/capas/livros",
        "supabase.storage.base-url-tccs=https://example.supabase.co/storage/v1/object/tcc",
        "app.cors.allowed-origins=http://localhost:5173"
})
class LumilivreApplicationTests {

	@Test
	void contextLoads() {
	}

}
