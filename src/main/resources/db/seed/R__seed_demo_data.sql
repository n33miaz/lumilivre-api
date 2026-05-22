-- =============================================================================
--  R__seed_demo_data.sql
-- -----------------------------------------------------------------------------
--  Dados sinteticos de demonstracao. Este seed e opt-in: inclua
--  classpath:db/seed em LUMILIVRE_FLYWAY_LOCATIONS quando quiser popular
--  ambiente local/dev.
-- =============================================================================

DO $$
DECLARE
    seed_table text;
BEGIN
    FOREACH seed_table IN ARRAY ARRAY[
        'student',
        'app_user',
        'book',
        'book_genre',
        'book_copy',
        'loan',
        'loan_request',
        'reservation',
        'thesis'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I DISABLE ROW LEVEL SECURITY', seed_table);
    END LOOP;
END $$;

-- ----------------------------------------------------------------------------
-- Students
-- ----------------------------------------------------------------------------
WITH refs AS (
    SELECT
        (SELECT id FROM course WHERE name = 'Desenvolvimento de Sistemas') AS systems_course_id,
        (SELECT id FROM course WHERE name = 'Administração') AS admin_course_id,
        (SELECT id FROM academic_module WHERE name = 'Módulo 2') AS module_2_id,
        (SELECT id FROM academic_module WHERE name = 'Módulo 3') AS module_3_id,
        (SELECT id FROM study_shift WHERE name = 'Matutino') AS morning_shift_id,
        (SELECT id FROM study_shift WHERE name = 'Noturno') AS evening_shift_id
)
INSERT INTO student (
    id,
    registration_number,
    full_name,
    cpf,
    birth_date,
    phone_number,
    email,
    course_id,
    academic_module_id,
    study_shift_id,
    postal_code,
    street,
    district,
    city,
    state_code,
    street_number
)
SELECT * FROM (
    SELECT
        '00000000-0000-4000-8000-000000002401'::uuid,
        '2024001',
        'Ana Beatriz Lima',
        '52998224725',
        DATE '2007-04-12',
        '11990010001',
        'ana.lima@example.com',
        refs.systems_course_id,
        refs.module_2_id,
        refs.morning_shift_id,
        '01001000',
        'Praca da Se',
        'Se',
        'Sao Paulo',
        'SP',
        100
    FROM refs
    UNION ALL
    SELECT
        '00000000-0000-4000-8000-000000002402'::uuid,
        '2024002',
        'Carlos Henrique Souza',
        '15350946056',
        DATE '2006-11-03',
        '11990010002',
        'carlos.souza@example.com',
        refs.admin_course_id,
        refs.module_3_id,
        refs.evening_shift_id,
        '20040002',
        'Rua da Quitanda',
        'Centro',
        'Rio de Janeiro',
        'RJ',
        42
    FROM refs
    UNION ALL
    SELECT
        '00000000-0000-4000-8000-000000002403'::uuid,
        '2024003',
        'Mariana Oliveira Santos',
        '11144477735',
        DATE '2008-02-20',
        '11990010003',
        'mariana.santos@example.com',
        refs.systems_course_id,
        refs.module_2_id,
        refs.morning_shift_id,
        '30140071',
        'Avenida Afonso Pena',
        'Centro',
        'Belo Horizonte',
        'MG',
        85
    FROM refs
) AS data (
    id,
    registration_number,
    full_name,
    cpf,
    birth_date,
    phone_number,
    email,
    course_id,
    academic_module_id,
    study_shift_id,
    postal_code,
    street,
    district,
    city,
    state_code,
    street_number
)
ON CONFLICT (registration_number) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    cpf = EXCLUDED.cpf,
    birth_date = EXCLUDED.birth_date,
    phone_number = EXCLUDED.phone_number,
    email = EXCLUDED.email,
    course_id = EXCLUDED.course_id,
    academic_module_id = EXCLUDED.academic_module_id,
    study_shift_id = EXCLUDED.study_shift_id,
    postal_code = EXCLUDED.postal_code,
    street = EXCLUDED.street,
    district = EXCLUDED.district,
    city = EXCLUDED.city,
    state_code = EXCLUDED.state_code,
    street_number = EXCLUDED.street_number;

-- ----------------------------------------------------------------------------
-- Users
-- Demo credentials:
--   admin@lumilivre.test / admin@lumilivre.test
--   librarian@lumilivre.test / 123456
--   2024001 / 2024001
-- ----------------------------------------------------------------------------
INSERT INTO app_user (id, email, password_hash, role, student_id, preferred_locale)
VALUES
    (
        '00000000-0000-4000-8000-000000001001'::uuid,
        'admin@lumilivre.test',
        '$2a$10$.gPKMreKYQf0en5npUJxau0lmiKjd9iTfQoTW1mN7z.BPkMAXN2Ay',
        'ADMIN',
        NULL,
        'pt-BR'
    ),
    (
        '00000000-0000-4000-8000-000000001002'::uuid,
        'librarian@lumilivre.test',
        '$2a$10$s0gF584h97IvpZgNzmNz9.ITIq4vhsnT4VZRbz.45XmfHWg3xNJdy',
        'LIBRARIAN',
        NULL,
        'pt-BR'
    ),
    (
        '00000000-0000-4000-8000-000000001003'::uuid,
        'ana.lima@example.com',
        '$2a$10$fHJ73JQxR0RhvAJVYA8ZtuoNyfup0aE1WML5B82x.VSkQigYppugK',
        'STUDENT',
        '00000000-0000-4000-8000-000000002401'::uuid,
        'pt-BR'
    )
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    student_id = EXCLUDED.student_id,
    preferred_locale = EXCLUDED.preferred_locale;

-- ----------------------------------------------------------------------------
-- Books and genres
-- ----------------------------------------------------------------------------
INSERT INTO book (
    id,
    isbn,
    title,
    publication_date,
    page_count,
    dewey_code,
    publisher,
    age_rating,
    edition,
    synopsis,
    author,
    cover_type,
    rating
)
VALUES
    (
        '00000000-0000-4000-8000-000000003001'::uuid,
        '9788535902778',
        'Dom Casmurro',
        DATE '1899-01-01',
        256,
        '869',
        'Editora Ficticia Escola',
        'GENERAL',
        '1a edicao demo',
        'Classico da literatura brasileira usado em atividades de leitura orientada.',
        'Machado de Assis',
        'PAPERBACK',
        4.8
    ),
    (
        '00000000-0000-4000-8000-000000003002'::uuid,
        '9788535914849',
        'O Cortico',
        DATE '1890-01-01',
        304,
        '869',
        'Editora Ficticia Escola',
        'TEEN',
        '1a edicao demo',
        'Romance naturalista para estudo de contexto historico e social.',
        'Aluisio Azevedo',
        'PAPERBACK',
        4.5
    ),
    (
        '00000000-0000-4000-8000-000000003003'::uuid,
        '9788575228412',
        'Logica de Programacao para Iniciantes',
        DATE '2023-02-15',
        220,
        '005',
        'LumiLivre Educacional',
        'GENERAL',
        '2a edicao demo',
        'Introducao sintetica a algoritmos, variaveis, estruturas condicionais e repeticao.',
        'Equipe LumiLivre',
        'SOFTCOVER',
        4.7
    )
ON CONFLICT (id) DO UPDATE SET
    isbn = EXCLUDED.isbn,
    title = EXCLUDED.title,
    publication_date = EXCLUDED.publication_date,
    page_count = EXCLUDED.page_count,
    dewey_code = EXCLUDED.dewey_code,
    publisher = EXCLUDED.publisher,
    age_rating = EXCLUDED.age_rating,
    edition = EXCLUDED.edition,
    synopsis = EXCLUDED.synopsis,
    author = EXCLUDED.author,
    cover_type = EXCLUDED.cover_type,
    rating = EXCLUDED.rating;

INSERT INTO book_genre (book_id, genre_id)
SELECT links.book_id, g.id
FROM (
    VALUES
        ('00000000-0000-4000-8000-000000003001'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003001'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003002'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003002'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003003'::uuid, 'Tecnologia'),
        ('00000000-0000-4000-8000-000000003003'::uuid, 'Didático')
) AS links(book_id, genre_name)
JOIN genre g ON g.name = links.genre_name
ON CONFLICT (book_id, genre_id) DO NOTHING;

INSERT INTO book_copy (id, copy_code, status, book_id, shelf_location)
VALUES
    (
        '00000000-0000-4000-8000-000000004001'::uuid,
        'LUM-0001',
        'AVAILABLE',
        '00000000-0000-4000-8000-000000003001'::uuid,
        'A1-01'
    ),
    (
        '00000000-0000-4000-8000-000000004002'::uuid,
        'LUM-0002',
        'BORROWED',
        '00000000-0000-4000-8000-000000003001'::uuid,
        'A1-02'
    ),
    (
        '00000000-0000-4000-8000-000000004003'::uuid,
        'LUM-0003',
        'AVAILABLE',
        '00000000-0000-4000-8000-000000003002'::uuid,
        'A2-01'
    ),
    (
        '00000000-0000-4000-8000-000000004004'::uuid,
        'LUM-0004',
        'AVAILABLE',
        '00000000-0000-4000-8000-000000003003'::uuid,
        'T1-01'
    )
ON CONFLICT (copy_code) DO UPDATE SET
    status = EXCLUDED.status,
    book_id = EXCLUDED.book_id,
    shelf_location = EXCLUDED.shelf_location;

-- ----------------------------------------------------------------------------
-- Operational data
-- ----------------------------------------------------------------------------
INSERT INTO loan (
    id,
    borrowed_at,
    due_at,
    returned_at,
    status,
    student_id,
    book_copy_id,
    renewal_count
)
VALUES
    (
        '00000000-0000-4000-8000-000000005001'::uuid,
        now() - INTERVAL '5 days',
        now() + INTERVAL '9 days',
        NULL,
        'ACTIVE',
        '00000000-0000-4000-8000-000000002401'::uuid,
        '00000000-0000-4000-8000-000000004002'::uuid,
        0
    ),
    (
        '00000000-0000-4000-8000-000000005002'::uuid,
        now() - INTERVAL '35 days',
        now() - INTERVAL '21 days',
        now() - INTERVAL '22 days',
        'COMPLETED',
        '00000000-0000-4000-8000-000000002402'::uuid,
        '00000000-0000-4000-8000-000000004003'::uuid,
        1
    )
ON CONFLICT (id) DO UPDATE SET
    borrowed_at = EXCLUDED.borrowed_at,
    due_at = EXCLUDED.due_at,
    returned_at = EXCLUDED.returned_at,
    status = EXCLUDED.status,
    student_id = EXCLUDED.student_id,
    book_copy_id = EXCLUDED.book_copy_id,
    renewal_count = EXCLUDED.renewal_count;

INSERT INTO loan_request (id, student_id, book_copy_id, requested_at, status, note)
VALUES
    (
        '00000000-0000-4000-8000-000000006001'::uuid,
        '00000000-0000-4000-8000-000000002403'::uuid,
        '00000000-0000-4000-8000-000000004004'::uuid,
        now() - INTERVAL '1 day',
        'PENDING',
        'Solicitado via seed demo'
    )
ON CONFLICT (id) DO UPDATE SET
    student_id = EXCLUDED.student_id,
    book_copy_id = EXCLUDED.book_copy_id,
    requested_at = EXCLUDED.requested_at,
    status = EXCLUDED.status,
    note = EXCLUDED.note;

INSERT INTO reservation (id, student_id, book_id, status, queue_position, expires_at, notified_at)
VALUES
    (
        '00000000-0000-4000-8000-000000007001'::uuid,
        '00000000-0000-4000-8000-000000002402'::uuid,
        '00000000-0000-4000-8000-000000003001'::uuid,
        'WAITING',
        1,
        NULL,
        NULL
    )
ON CONFLICT (id) DO UPDATE SET
    student_id = EXCLUDED.student_id,
    book_id = EXCLUDED.book_id,
    status = EXCLUDED.status,
    queue_position = EXCLUDED.queue_position,
    expires_at = EXCLUDED.expires_at,
    notified_at = EXCLUDED.notified_at;

INSERT INTO thesis (
    id,
    title,
    authors,
    advisors,
    course_id,
    completion_year,
    completion_semester,
    external_url,
    is_active
)
SELECT
    '00000000-0000-4000-8000-000000008001'::uuid,
    'Sistema de Catalogo Digital para Biblioteca Escolar',
    'Ana Beatriz Lima; Mariana Oliveira Santos',
    'Prof. Joao Pereira',
    c.id,
    2025,
    '2',
    'https://example.com/lumilivre/tcc-demo',
    TRUE
FROM course c
WHERE c.name = 'Desenvolvimento de Sistemas'
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    authors = EXCLUDED.authors,
    advisors = EXCLUDED.advisors,
    course_id = EXCLUDED.course_id,
    completion_year = EXCLUDED.completion_year,
    completion_semester = EXCLUDED.completion_semester,
    external_url = EXCLUDED.external_url,
    is_active = EXCLUDED.is_active;

REFRESH MATERIALIZED VIEW mv_dashboard_stats;
REFRESH MATERIALIZED VIEW mv_top_books;
REFRESH MATERIALIZED VIEW mv_loans_by_month;

DO $$
DECLARE
    seed_table text;
BEGIN
    FOREACH seed_table IN ARRAY ARRAY[
        'student',
        'app_user',
        'book',
        'book_genre',
        'book_copy',
        'loan',
        'loan_request',
        'reservation',
        'thesis'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', seed_table);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', seed_table);
    END LOOP;
END $$;
