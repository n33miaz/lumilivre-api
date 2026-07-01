-- =============================================================================
--  V4__create_indexes_and_search.sql
-- -----------------------------------------------------------------------------
--  Indices de performance e busca textual.
-- =============================================================================

-- ----------------------------------------------------------------------------
-- Indices basicos de FK e filtros frequentes
-- ----------------------------------------------------------------------------

-- reader
CREATE INDEX idx_reader_course_module_shift
    ON reader (course_id, academic_module_id, study_shift_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_reader_registration_number
    ON reader (registration_number)
    WHERE deleted_at IS NULL;

-- book
CREATE INDEX idx_book_dewey_code
    ON book (dewey_code)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_book_publisher
    ON book (publisher)
    WHERE deleted_at IS NULL;

-- book_copy
CREATE INDEX idx_book_copy_book_id_status
    ON book_copy (book_id, status)
    WHERE deleted_at IS NULL;

-- loan
CREATE INDEX idx_loan_reader_id_status_due_at
    ON loan (reader_id, status, due_at);

CREATE INDEX idx_loan_book_copy_id_status
    ON loan (book_copy_id, status);

CREATE INDEX idx_loan_status_due_at
    ON loan (status, due_at);

CREATE INDEX idx_loan_borrowed_at
    ON loan (borrowed_at);

-- loan_request
CREATE INDEX idx_loan_request_reader_id_status_requested_at
    ON loan_request (reader_id, status, requested_at);

CREATE INDEX idx_loan_request_status
    ON loan_request (status);

-- reservation
CREATE INDEX idx_reservation_book_id_status_queue_position
    ON reservation (book_id, status, queue_position);

CREATE INDEX idx_reservation_reader_id
    ON reservation (reader_id);

-- Regra: um leitor nao pode ter duas reservas ATIVAS do mesmo livro.
CREATE UNIQUE INDEX uq_reservation_active_reader_book
    ON reservation (reader_id, book_id)
    WHERE status IN ('WAITING', 'READY');

-- thesis
CREATE INDEX idx_thesis_course_id
    ON thesis (course_id)
    WHERE deleted_at IS NULL AND is_active = TRUE;

-- outbox_event
CREATE INDEX idx_outbox_event_status_next_retry_at_created_at
    ON outbox_event (status, next_retry_at NULLS FIRST, created_at);

-- audit_log
CREATE INDEX idx_audit_log_occurred_at
    ON audit_log (occurred_at DESC);

CREATE INDEX idx_audit_log_actor
    ON audit_log (actor);

CREATE INDEX idx_audit_log_action
    ON audit_log (action);

-- ----------------------------------------------------------------------------
-- Busca textual: trigram (ILIKE) + tsvector (FTS)
-- ----------------------------------------------------------------------------

-- Trigram GIN em nomes/titulos/autores (ILIKE com acento tolerante).
CREATE INDEX idx_reader_full_name_trgm
    ON reader USING gin (immutable_unaccent(full_name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_book_title_trgm
    ON book USING gin (immutable_unaccent(title) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_book_author_trgm
    ON book USING gin (immutable_unaccent(coalesce(author, '')) gin_trgm_ops)
    WHERE deleted_at IS NULL;

-- Full-text search no catalogo (titulo + autor + sinopse, em portugues).
CREATE INDEX idx_book_fts
    ON book USING gin (
        to_tsvector(
            'portuguese',
            immutable_unaccent(
                coalesce(title,   '') || ' ' ||
                coalesce(author,  '') || ' ' ||
                coalesce(synopsis,'')
            )
        )
    )
    WHERE deleted_at IS NULL;
