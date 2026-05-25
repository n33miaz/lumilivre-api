package br.com.lumilivre.api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.model.LoanRequest;
import br.com.lumilivre.api.model.OutboxEvent;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.LoanRequestRepository;
import br.com.lumilivre.api.repository.OutboxEventRepository;
import br.com.lumilivre.api.service.LoanRequestService;

/**
 * E2E focado no ciclo de empréstimo: aluno solicita → admin aprova → loan
 * fica ACTIVE, exemplar BORROWED, outbox e audit persistidos.
 *
 * <p>Reusa a seed demo (R__seed_demo_data.sql) — pega uma solicitação PENDING
 * já existente e aceita via {@link LoanRequestService#processarSolicitacao}.
 * Skip silencioso quando Docker não está disponível.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.scheduling.enabled=false",
        "jwt.secret=integration-test-secret-with-enough-length-for-hmac-signature-aaaaa",
        "supabase.url=http://localhost:9999",
        "supabase.key=test",
        "supabase.service-role.key=test",
        "lumilivre.storage.provider=local",
        "lumilivre.storage.local.base-dir=./build/storage-integration",
        "app.cors.allowed-origins=http://localhost:5173",
        "spring.mail.host=localhost",
        "spring.mail.port=1025",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class LoanFullFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("lumilivre_e2e")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final UUID PENDING_REQUEST_ID =
            UUID.fromString("00000000-0000-4000-8000-000000006001");
    private static final UUID TARGET_COPY_ID =
            UUID.fromString("00000000-0000-4000-8000-000000004004");

    @Autowired
    private LoanRequestService loanRequestService;

    @Autowired
    private LoanRequestRepository loanRequestRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Test
    void acceptingPendingRequestCreatesActiveLoanAndOutbox() {
        LoanRequest pendingBefore = loanRequestRepository.findById(PENDING_REQUEST_ID).orElseThrow();
        assertThat(pendingBefore.getStatus()).isEqualTo(LoanRequestStatus.PENDING);

        BookCopy copyBefore = bookCopyRepository.findById(TARGET_COPY_ID).orElseThrow();
        assertThat(copyBefore.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);

        long outboxBefore = outboxRepository.count();

        loanRequestService.processarSolicitacao(PENDING_REQUEST_ID, true);

        LoanRequest pendingAfter = loanRequestRepository.findById(PENDING_REQUEST_ID).orElseThrow();
        assertThat(pendingAfter.getStatus()).isEqualTo(LoanRequestStatus.ACCEPTED);

        BookCopy copyAfter = bookCopyRepository.findById(TARGET_COPY_ID).orElseThrow();
        assertThat(copyAfter.getStatus()).isEqualTo(BookCopyStatus.BORROWED);

        List<Loan> loansForStudent = loanRepository.findAll().stream()
                .filter(l -> l.getStudent().getRegistrationNumber()
                        .equals(pendingAfter.getStudent().getRegistrationNumber()))
                .filter(l -> l.getBookCopy().getId().equals(TARGET_COPY_ID))
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .toList();
        assertThat(loansForStudent).hasSize(1);

        List<OutboxEvent> newEvents = outboxRepository.findAll().stream()
                .skip(outboxBefore)
                .toList();
        assertThat(newEvents)
                .extracting(OutboxEvent::getEventType)
                .contains(EventType.REQUEST_ACCEPTED, EventType.LOAN_CREATED);
    }
}
