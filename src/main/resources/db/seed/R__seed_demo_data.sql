-- =============================================================================
--  R__seed_demo_data.sql
-- -----------------------------------------------------------------------------
--  Dados sinteticos de demonstracao. Este seed e opt-in: inclua
--  classpath:db/seed em LUMILIVRE_FLYWAY_LOCATIONS quando quiser popular
--  ambiente local/dev.
--
--  Cobre todos os status de cada entidade para exercitar a UI completa.
--  Base explicita (nomeada) + expansao WS-06 (generate_series deterministico):
--    - reader      (50): penalidades variadas (11), com/sem foto, todos os cursos
--    - app_user     (4): admin, librarian, studant (1a senha+tour), librarian2 (1a senha)
--    - book        (100): 92 com capa / 8 sem (fallback), todos os generos
--    - book_copy   (150): AVAILABLE 98 / BORROWED 30 / MAINTENANCE 11 / UNAVAILABLE 11
--    - loan         (60): ACTIVE 24 (vencendo em 1-13d), OVERDUE 6, COMPLETED 30 (~13 meses)
--    - loan_request (15): PENDING / ACCEPTED / REJECTED / CANCELLED
--    - reservation  (15): fila FIFO x3 no mesmo livro, READY, FULFILLED, EXPIRED, CANCELLED
--    - app_content  (14): todos os tipos/estados/segmentacao/janelas
--    - access_log   (50) / audit_log (15) / outbox_event (5)
-- =============================================================================

DO $$
DECLARE
    seed_table text;
BEGIN
    FOREACH seed_table IN ARRAY ARRAY[
        'reader',
        'library_settings',
        'app_user',
        'book',
        'book_genre',
        'book_copy',
        'loan',
        'loan_request',
        'reservation',
        'app_content',
        'access_log',
        'audit_log',
        'outbox_event'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I DISABLE ROW LEVEL SECURITY', seed_table);
    END LOOP;
END $$;

-- ----------------------------------------------------------------------------
-- Readers (8)
-- ----------------------------------------------------------------------------
INSERT INTO library_settings (id, library_type)
VALUES (TRUE, 'SCHOOL')
ON CONFLICT (id) DO UPDATE SET library_type = EXCLUDED.library_type;

INSERT INTO reader (
    id, registration_number, full_name, cpf, birth_date, phone_number, email,
    course_id, academic_module_id, study_shift_id,
    postal_code, street, district, city, state_code, street_number,
    penalty_code, penalty_expires_at
)
VALUES
    ('00000000-0000-4000-8000-000000002401'::uuid, '2024001', 'Ana Beatriz Lima',     '52998224725', DATE '2007-04-12', '11990010001', 'ana.lima@example.com',
        (SELECT id FROM course WHERE name = 'Desenvolvimento de Sistemas'),
        (SELECT id FROM academic_module WHERE name = 'Módulo 2'),
        (SELECT id FROM study_shift WHERE name = 'Matutino'),
        '01001000', 'Praca da Se',           'Se',      'Sao Paulo',      'SP', 100, NULL, NULL),
    ('00000000-0000-4000-8000-000000002402'::uuid, '2024002', 'Carlos Henrique Souza','15350946056', DATE '2006-11-03', '11990010002', 'carlos.souza@example.com',
        (SELECT id FROM course WHERE name = 'Administração'),
        (SELECT id FROM academic_module WHERE name = 'Módulo 3'),
        (SELECT id FROM study_shift WHERE name = 'Noturno'),
        '20040002', 'Rua da Quitanda',       'Centro',  'Rio de Janeiro', 'RJ',  42, NULL, NULL),
    ('00000000-0000-4000-8000-000000002403'::uuid, '2024003', 'Mariana Oliveira Santos','11144477735', DATE '2008-02-20', '11990010003', 'mariana.santos@example.com',
        (SELECT id FROM course WHERE name = 'Desenvolvimento de Sistemas'),
        (SELECT id FROM academic_module WHERE name = 'Módulo 2'),
        (SELECT id FROM study_shift WHERE name = 'Matutino'),
        '30140071', 'Avenida Afonso Pena',   'Centro',  'Belo Horizonte', 'MG',  85, NULL, NULL),
    ('00000000-0000-4000-8000-000000002404'::uuid, '2024004', 'Pedro Henrique Costa', '39053344705', DATE '2007-07-15', '11990010004', 'pedro.costa@example.com',
        (SELECT id FROM course WHERE name = 'Técnico em Mecatrônica'),
        (SELECT id FROM academic_module WHERE name = 'Módulo 3'),
        (SELECT id FROM study_shift WHERE name = 'Vespertino'),
        '40060001', 'Avenida Sete de Setembro','Centro','Salvador',      'BA', 150, NULL, NULL),
    ('00000000-0000-4000-8000-000000002405'::uuid, '2024005', 'Juliana Ferreira Almeida','85345367040', DATE '2006-05-09', '11990010005', 'juliana.almeida@example.com',
        (SELECT id FROM course WHERE name = 'Recursos Humanos'),
        (SELECT id FROM academic_module WHERE name = 'Módulo 4'),
        (SELECT id FROM study_shift WHERE name = 'Noturno'),
        '80020110', 'Rua XV de Novembro',    'Centro',  'Curitiba',       'PR',  22, NULL, NULL),
    ('00000000-0000-4000-8000-000000002406'::uuid, '2024006', 'Rafael Mendes Silva',  '03992377007', DATE '2008-10-30', '11990010006', 'rafael.mendes@example.com',
        (SELECT id FROM course WHERE name = 'Logística'),
        (SELECT id FROM academic_module WHERE name = 'Módulo 1'),
        (SELECT id FROM study_shift WHERE name = 'Matutino'),
        '90010321', 'Rua dos Andradas',      'Centro',  'Porto Alegre',   'RS',  77, NULL, NULL),
    ('00000000-0000-4000-8000-000000002407'::uuid, '2024007', 'Bruna Fernandes',      '37820089079', DATE '2007-01-25', '11990010007', 'bruna.fernandes@example.com',
        (SELECT id FROM course WHERE name = 'Técnico em Contabilidade'),
        (SELECT id FROM academic_module WHERE name = 'Módulo 3'),
        (SELECT id FROM study_shift WHERE name = 'Noturno'),
        '60060000', 'Avenida Beira Mar',     'Meireles','Fortaleza',      'CE',  10, 'BLOCK',   now() + INTERVAL '6 days'),
    ('00000000-0000-4000-8000-000000002408'::uuid, '2024008', 'Lucas Pereira Cardoso','30864180024', DATE '2008-09-18', '11990010008', 'lucas.pereira@example.com',
        (SELECT id FROM course WHERE name = 'Técnico em Enfermagem'),
        (SELECT id FROM academic_module WHERE name = 'Módulo 2'),
        (SELECT id FROM study_shift WHERE name = 'Vespertino'),
        '50070000', 'Avenida Boa Viagem',    'Boa Viagem','Recife',       'PE',  55, 'WARNING', now() + INTERVAL '1 day')
ON CONFLICT (registration_number) DO UPDATE SET
    full_name           = EXCLUDED.full_name,
    cpf                 = EXCLUDED.cpf,
    birth_date          = EXCLUDED.birth_date,
    phone_number        = EXCLUDED.phone_number,
    email               = EXCLUDED.email,
    course_id           = EXCLUDED.course_id,
    academic_module_id  = EXCLUDED.academic_module_id,
    study_shift_id      = EXCLUDED.study_shift_id,
    postal_code         = EXCLUDED.postal_code,
    street              = EXCLUDED.street,
    district            = EXCLUDED.district,
    city                = EXCLUDED.city,
    state_code          = EXCLUDED.state_code,
    street_number       = EXCLUDED.street_number,
    penalty_code        = EXCLUDED.penalty_code,
    penalty_expires_at  = EXCLUDED.penalty_expires_at;

-- ----------------------------------------------------------------------------
-- Users
-- Demo credentials (bcrypt hashes correspondem ao texto literal abaixo):
--   admin     / admin
--   librarian / librarian
--   2024001..2024008         / matricula igual (senha inicial = matricula)
-- Os hashes foram gerados uma vez para os tres usuarios base; leitores
-- novos ainda nao tem conta (cria-se via fluxo normal). Mantemos so as
-- credenciais demo abaixo para nao explodir esse seed com hashes BCrypt.
-- ----------------------------------------------------------------------------
-- Flags de onboarding (WS-10): studant demonstra 1ª senha (must_change) + tour pendente;
-- librarian tem tour pendente; admin já concluiu o tour.
INSERT INTO app_user (id, email, password_hash, role, reader_id, preferred_locale,
                      must_change_password, guided_tour_completed)
VALUES
    ('00000000-0000-4000-8000-000000001001'::uuid, 'admin',
        '$2b$12$SkU.zh6vrNj8dt0sdgZjSOMGxEFBTfqxCOtsX7wYed9CX0yCBmXrm',
        'ADMIN', NULL, 'pt-BR', FALSE, TRUE),
    ('00000000-0000-4000-8000-000000001002'::uuid, 'librarian',
        '$2b$12$PGsQqsJvPgZkTgh3cGJ7qu6744vKKJ7PL0QRg12LY5p38be7s5jBe',
        'LIBRARIAN', NULL, 'pt-BR', FALSE, FALSE),
    ('00000000-0000-4000-8000-000000001003'::uuid, 'studant',
        '$2a$10$fHJ73JQxR0RhvAJVYA8ZtuoNyfup0aE1WML5B82x.VSkQigYppugK',
        'READER', '00000000-0000-4000-8000-000000002401'::uuid, 'pt-BR', TRUE, FALSE)
ON CONFLICT (email) DO UPDATE SET
    password_hash         = EXCLUDED.password_hash,
    role                  = EXCLUDED.role,
    reader_id            = EXCLUDED.reader_id,
    preferred_locale      = EXCLUDED.preferred_locale,
    must_change_password  = EXCLUDED.must_change_password,
    guided_tour_completed = EXCLUDED.guided_tour_completed;

-- ----------------------------------------------------------------------------
-- Books (30)
-- cover_url usa o servico publico OpenLibrary (placeholder grafico quando o
-- ISBN nao existe). Volume e edicao ficam neutros para evitar acentos.
-- ----------------------------------------------------------------------------
INSERT INTO book (
    id, isbn, title, publication_date, page_count, dewey_code, publisher,
    age_rating, edition, volume, synopsis, author, cover_type, cover_url, rating
)
VALUES
    ('00000000-0000-4000-8000-000000003001'::uuid, '9788535902778', 'Dom Casmurro',                          DATE '1899-01-01', 256, '869', 'Editora Ficticia Escola',    'GENERAL', '1a edicao demo', NULL, 'Classico da literatura brasileira usado em atividades de leitura orientada.', 'Machado de Assis',         'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788535902778-L.jpg', 4.8),
    ('00000000-0000-4000-8000-000000003002'::uuid, '9788535914849', 'O Cortico',                              DATE '1890-01-01', 304, '869', 'Editora Ficticia Escola',    'TEEN',    '1a edicao demo', NULL, 'Romance naturalista para estudo de contexto historico e social.',              'Aluisio Azevedo',          'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788535914849-L.jpg', 4.5),
    ('00000000-0000-4000-8000-000000003003'::uuid, '9788575228412', 'Logica de Programacao para Iniciantes',  DATE '2023-02-15', 220, '005', 'LumiLivre Educacional',      'GENERAL', '2a edicao demo', NULL, 'Introducao a algoritmos, variaveis, estruturas condicionais e repeticao.',     'Equipe LumiLivre',         'SOFTCOVER', 'https://covers.openlibrary.org/b/isbn/9788575228412-L.jpg', 4.7),
    ('00000000-0000-4000-8000-000000003004'::uuid, '9780132350884', 'Clean Code',                             DATE '2008-08-01', 464, '005', 'Pearson',                    'GENERAL', '1st demo',       NULL, 'Manual pratico sobre escrita de codigo legivel e sustentavel.',                'Robert C. Martin',         'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg', 4.7),
    ('00000000-0000-4000-8000-000000003005'::uuid, '9780201633610', 'Design Patterns',                        DATE '1994-10-21', 395, '005', 'Addison-Wesley',             'GENERAL', '1st demo',       NULL, 'Catalogo classico de padroes de projeto orientados a objetos.',                'Gamma, Helm, Johnson, Vlissides', 'HARDCOVER', 'https://covers.openlibrary.org/b/isbn/9780201633610-L.jpg', 4.6),
    ('00000000-0000-4000-8000-000000003006'::uuid, '9780321125217', 'Domain-Driven Design',                   DATE '2003-08-30', 560, '005', 'Addison-Wesley',             'GENERAL', '1st demo',       NULL, 'Modelagem estrategica e tatica orientada ao dominio do negocio.',              'Eric Evans',               'HARDCOVER', 'https://covers.openlibrary.org/b/isbn/9780321125217-L.jpg', 4.6),
    ('00000000-0000-4000-8000-000000003007'::uuid, '9788525906670', 'O Pequeno Principe',                     DATE '1943-04-06', 96,  '843', 'Editora Ficticia Escola',    'GENERAL', '1a edicao demo', NULL, 'Fabula filosofica universalmente amada por todas as idades.',                  'Antoine de Saint-Exupery', 'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788525906670-L.jpg', 4.9),
    ('00000000-0000-4000-8000-000000003008'::uuid, '9788535929751', 'Vidas Secas',                            DATE '1938-01-01', 175, '869', 'Editora Ficticia Escola',    'TEEN',    '1a edicao demo', NULL, 'Romance regionalista da geracao de 30 ambientado no sertao nordestino.',       'Graciliano Ramos',         'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788535929751-L.jpg', 4.4),
    ('00000000-0000-4000-8000-000000003009'::uuid, '9788580631753', 'Memorias Postumas de Bras Cubas',        DATE '1881-01-01', 256, '869', 'Editora Ficticia Escola',    'TEEN',    '1a edicao demo', NULL, 'Romance machadiano narrado por um defunto-autor.',                             'Machado de Assis',         'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788580631753-L.jpg', 4.7),
    ('00000000-0000-4000-8000-000000003010'::uuid, '9788525066404', 'Iracema',                                DATE '1865-01-01', 144, '869', 'Editora Ficticia Escola',    'GENERAL', '1a edicao demo', NULL, 'Romance indianista com paisagens do Ceara colonial.',                          'Jose de Alencar',          'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788525066404-L.jpg', 4.3),
    ('00000000-0000-4000-8000-000000003011'::uuid, '9788525049810', 'Macunaima',                              DATE '1928-01-01', 208, '869', 'Editora Ficticia Escola',    'TEEN',    '1a edicao demo', NULL, 'Rapsodia modernista com o heroi sem nenhum carater.',                          'Mario de Andrade',         'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788525049810-L.jpg', 4.2),
    ('00000000-0000-4000-8000-000000003012'::uuid, '9788535929386', 'Capitaes da Areia',                      DATE '1937-01-01', 280, '869', 'Editora Ficticia Escola',    'TEEN',    '1a edicao demo', NULL, 'Drama social com a vida de meninos de rua em Salvador.',                       'Jorge Amado',              'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788535929386-L.jpg', 4.5),
    ('00000000-0000-4000-8000-000000003013'::uuid, '9780451524935', '1984',                                   DATE '1949-06-08', 328, '823', 'Penguin Demo',               'TEEN',    '1st demo',       NULL, 'Distopia sobre vigilancia, totalitarismo e linguagem.',                        'George Orwell',            'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9780451524935-L.jpg', 4.7),
    ('00000000-0000-4000-8000-000000003014'::uuid, '9780062315007', 'The Alchemist',                          DATE '1988-01-01', 208, '869', 'HarperOne Demo',             'GENERAL', '1st demo',       NULL, 'Jornada de auto-descoberta de um jovem pastor andaluz.',                       'Paulo Coelho',             'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9780062315007-L.jpg', 4.4),
    ('00000000-0000-4000-8000-000000003015'::uuid, '9780743273565', 'The Great Gatsby',                       DATE '1925-04-10', 180, '813', 'Scribner Demo',              'TEEN',    '1st demo',       NULL, 'Critica social ao sonho americano nos anos 1920.',                             'F. Scott Fitzgerald',      'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9780743273565-L.jpg', 4.3),
    ('00000000-0000-4000-8000-000000003016'::uuid, '9780061120084', 'To Kill a Mockingbird',                  DATE '1960-07-11', 281, '813', 'HarperCollins Demo',         'TEEN',    '1st demo',       NULL, 'Romance sobre justica racial visto pelos olhos de Scout Finch.',               'Harper Lee',               'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9780061120084-L.jpg', 4.6),
    ('00000000-0000-4000-8000-000000003017'::uuid, '9780553573404', 'A Game of Thrones',                      DATE '1996-08-01', 694, '823', 'Bantam Demo',                'ADULT',   '1st demo',       1,    'Primeiro volume da saga As Cronicas de Gelo e Fogo.',                          'George R. R. Martin',      'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9780553573404-L.jpg', 4.7),
    ('00000000-0000-4000-8000-000000003018'::uuid, '9780553579901', 'A Clash of Kings',                       DATE '1998-11-16', 768, '823', 'Bantam Demo',                'ADULT',   '1st demo',       2,    'Segundo volume da saga As Cronicas de Gelo e Fogo.',                           'George R. R. Martin',      'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9780553579901-L.jpg', 4.7),
    ('00000000-0000-4000-8000-000000003019'::uuid, '9780747532699', 'Harry Potter and the Philosophers Stone',DATE '1997-06-26', 223, '823', 'Bloomsbury Demo',            'GENERAL', '1st demo',       1,    'Primeiro livro da saga Harry Potter.',                                         'J. K. Rowling',            'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9780747532699-L.jpg', 4.9),
    ('00000000-0000-4000-8000-000000003020'::uuid, '9780395489321', 'The Hobbit',                             DATE '1937-09-21', 310, '823', 'Allen & Unwin Demo',         'GENERAL', '1st demo',       NULL, 'Bilbo Baggins parte em sua inesperada aventura pela Terra-media.',             'J. R. R. Tolkien',         'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9780395489321-L.jpg', 4.8),
    ('00000000-0000-4000-8000-000000003021'::uuid, '9788580573466', 'Sapiens',                                DATE '2011-01-01', 464, '909', 'L&PM Demo',                  'GENERAL', '1a edicao demo', NULL, 'Uma breve historia da humanidade pela perspectiva evolutiva.',                 'Yuval Noah Harari',        'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788580573466-L.jpg', 4.6),
    ('00000000-0000-4000-8000-000000003022'::uuid, '9788535930931', 'Quincas Borba',                          DATE '1891-01-01', 320, '869', 'Editora Ficticia Escola',    'TEEN',    '1a edicao demo', NULL, 'Romance da fase realista de Machado de Assis.',                                'Machado de Assis',         'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788535930931-L.jpg', 4.5),
    ('00000000-0000-4000-8000-000000003023'::uuid, '9788583510178', 'O Mundo de Sofia',                       DATE '1991-01-01', 555, '109', 'Editora Ficticia Demo',      'GENERAL', '1a edicao demo', NULL, 'Romance que conta a historia da filosofia ocidental.',                         'Jostein Gaarder',          'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788583510178-L.jpg', 4.4),
    ('00000000-0000-4000-8000-000000003024'::uuid, '9788535914872', 'Sagarana',                               DATE '1946-01-01', 432, '869', 'Editora Ficticia Escola',    'GENERAL', '1a edicao demo', NULL, 'Coletanea de contos do sertao mineiro.',                                       'Joao Guimaraes Rosa',      'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788535914872-L.jpg', 4.3),
    ('00000000-0000-4000-8000-000000003025'::uuid, '9788535929393', 'A Hora da Estrela',                      DATE '1977-01-01', 87,  '869', 'Editora Ficticia Escola',    'TEEN',    '1a edicao demo', NULL, 'Ultimo romance publicado em vida de Clarice Lispector.',                       'Clarice Lispector',        'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788535929393-L.jpg', 4.4),
    ('00000000-0000-4000-8000-000000003026'::uuid, '9788525432025', 'O Diario de Anne Frank',                 DATE '1947-06-25', 320, '940', 'Record Demo',                'TEEN',    '1a edicao demo', NULL, 'Relato em primeira pessoa de uma jovem judia durante a Segunda Guerra.',       'Anne Frank',               'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788525432025-L.jpg', 4.7),
    ('00000000-0000-4000-8000-000000003027'::uuid, '9788525407221', 'Helena',                                 DATE '1876-01-01', 240, '869', 'Editora Ficticia Escola',    'TEEN',    '1a edicao demo', NULL, 'Romance da fase romantica de Machado de Assis.',                               'Machado de Assis',         'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788525407221-L.jpg', 4.1),
    ('00000000-0000-4000-8000-000000003028'::uuid, '9780321356680', 'Effective Java',                         DATE '2017-12-27', 416, '005', 'Addison-Wesley',             'GENERAL', '3rd demo',       NULL, 'Boas praticas para escrever Java elegante e idiomatico.',                      'Joshua Bloch',             'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9780321356680-L.jpg', 4.8),
    ('00000000-0000-4000-8000-000000003029'::uuid, '9780596007126', 'Head First Design Patterns',             DATE '2004-10-25', 694, '005', 'OReilly Demo',               'GENERAL', '1st demo',       NULL, 'Aprendizado visual de padroes de projeto.',                                    'Freeman, Robson',          'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9780596007126-L.jpg', 4.6),
    ('00000000-0000-4000-8000-000000003030'::uuid, '9788595080935', 'O Guia do Mochileiro das Galaxias',      DATE '1979-10-12', 208, '823', 'Editora Ficticia Demo',      'TEEN',    '1a edicao demo', NULL, 'Comedia de ficcao cientifica que comeca com a destruicao da Terra.',           'Douglas Adams',            'PAPERBACK', 'https://covers.openlibrary.org/b/isbn/9788595080935-L.jpg', 4.5)
ON CONFLICT (id) DO UPDATE SET
    isbn             = EXCLUDED.isbn,
    title            = EXCLUDED.title,
    publication_date = EXCLUDED.publication_date,
    page_count       = EXCLUDED.page_count,
    dewey_code       = EXCLUDED.dewey_code,
    publisher        = EXCLUDED.publisher,
    age_rating       = EXCLUDED.age_rating,
    edition          = EXCLUDED.edition,
    volume           = EXCLUDED.volume,
    synopsis         = EXCLUDED.synopsis,
    author           = EXCLUDED.author,
    cover_type       = EXCLUDED.cover_type,
    cover_url        = EXCLUDED.cover_url,
    rating           = EXCLUDED.rating;

-- ----------------------------------------------------------------------------
-- Book <-> Genre (10+ generos exercitados)
-- ----------------------------------------------------------------------------
INSERT INTO book_genre (book_id, genre_id)
SELECT links.book_id, g.id
FROM (
    VALUES
        ('00000000-0000-4000-8000-000000003001'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003001'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003002'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003002'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003003'::uuid, 'Tecnologia'),
        ('00000000-0000-4000-8000-000000003003'::uuid, 'Didático'),
        ('00000000-0000-4000-8000-000000003004'::uuid, 'Tecnologia'),
        ('00000000-0000-4000-8000-000000003004'::uuid, 'Didático'),
        ('00000000-0000-4000-8000-000000003005'::uuid, 'Tecnologia'),
        ('00000000-0000-4000-8000-000000003006'::uuid, 'Tecnologia'),
        ('00000000-0000-4000-8000-000000003007'::uuid, 'Infantojuvenil'),
        ('00000000-0000-4000-8000-000000003007'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003008'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003008'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003009'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003009'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003010'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003010'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003011'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003012'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003012'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003013'::uuid, 'Ficção Científica'),
        ('00000000-0000-4000-8000-000000003013'::uuid, 'Suspense'),
        ('00000000-0000-4000-8000-000000003014'::uuid, 'Autoajuda'),
        ('00000000-0000-4000-8000-000000003014'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003015'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003016'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003017'::uuid, 'Fantasia'),
        ('00000000-0000-4000-8000-000000003018'::uuid, 'Fantasia'),
        ('00000000-0000-4000-8000-000000003019'::uuid, 'Fantasia'),
        ('00000000-0000-4000-8000-000000003019'::uuid, 'Infantojuvenil'),
        ('00000000-0000-4000-8000-000000003020'::uuid, 'Fantasia'),
        ('00000000-0000-4000-8000-000000003020'::uuid, 'Infantojuvenil'),
        ('00000000-0000-4000-8000-000000003021'::uuid, 'História'),
        ('00000000-0000-4000-8000-000000003021'::uuid, 'Biografia'),
        ('00000000-0000-4000-8000-000000003022'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003022'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003023'::uuid, 'Educação'),
        ('00000000-0000-4000-8000-000000003024'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003025'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003025'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003026'::uuid, 'Biografia'),
        ('00000000-0000-4000-8000-000000003026'::uuid, 'História'),
        ('00000000-0000-4000-8000-000000003027'::uuid, 'Clássicos'),
        ('00000000-0000-4000-8000-000000003027'::uuid, 'Romance'),
        ('00000000-0000-4000-8000-000000003028'::uuid, 'Tecnologia'),
        ('00000000-0000-4000-8000-000000003029'::uuid, 'Tecnologia'),
        ('00000000-0000-4000-8000-000000003029'::uuid, 'Didático'),
        ('00000000-0000-4000-8000-000000003030'::uuid, 'Ficção Científica'),
        ('00000000-0000-4000-8000-000000003030'::uuid, 'Suspense')
) AS links(book_id, genre_name)
JOIN genre g ON g.name = links.genre_name
ON CONFLICT (book_id, genre_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- Book copies (15)
-- 4001, 4003-4008, 4012 AVAILABLE; 4002, 4009-4011 BORROWED (loans ativos),
-- 4013 BORROWED OVERDUE, 4014 MAINTENANCE, 4015 UNAVAILABLE
-- ----------------------------------------------------------------------------
INSERT INTO book_copy (id, copy_code, status, book_id, shelf_location)
VALUES
    ('00000000-0000-4000-8000-000000004001'::uuid, 'LUM-0001', 'AVAILABLE',   '00000000-0000-4000-8000-000000003001'::uuid, 'A1-01'),
    ('00000000-0000-4000-8000-000000004002'::uuid, 'LUM-0002', 'BORROWED',    '00000000-0000-4000-8000-000000003001'::uuid, 'A1-02'),
    ('00000000-0000-4000-8000-000000004003'::uuid, 'LUM-0003', 'AVAILABLE',   '00000000-0000-4000-8000-000000003002'::uuid, 'A2-01'),
    ('00000000-0000-4000-8000-000000004004'::uuid, 'LUM-0004', 'AVAILABLE',   '00000000-0000-4000-8000-000000003003'::uuid, 'T1-01'),
    ('00000000-0000-4000-8000-000000004005'::uuid, 'LUM-0005', 'AVAILABLE',   '00000000-0000-4000-8000-000000003004'::uuid, 'T1-02'),
    ('00000000-0000-4000-8000-000000004006'::uuid, 'LUM-0006', 'AVAILABLE',   '00000000-0000-4000-8000-000000003007'::uuid, 'I1-01'),
    ('00000000-0000-4000-8000-000000004007'::uuid, 'LUM-0007', 'AVAILABLE',   '00000000-0000-4000-8000-000000003013'::uuid, 'F1-01'),
    ('00000000-0000-4000-8000-000000004008'::uuid, 'LUM-0008', 'AVAILABLE',   '00000000-0000-4000-8000-000000003019'::uuid, 'F1-02'),
    ('00000000-0000-4000-8000-000000004009'::uuid, 'LUM-0009', 'BORROWED',    '00000000-0000-4000-8000-000000003008'::uuid, 'C1-01'),
    ('00000000-0000-4000-8000-000000004010'::uuid, 'LUM-0010', 'BORROWED',    '00000000-0000-4000-8000-000000003014'::uuid, 'R1-01'),
    ('00000000-0000-4000-8000-000000004011'::uuid, 'LUM-0011', 'BORROWED',    '00000000-0000-4000-8000-000000003020'::uuid, 'F1-03'),
    ('00000000-0000-4000-8000-000000004012'::uuid, 'LUM-0012', 'AVAILABLE',   '00000000-0000-4000-8000-000000003021'::uuid, 'H1-01'),
    ('00000000-0000-4000-8000-000000004013'::uuid, 'LUM-0013', 'BORROWED',    '00000000-0000-4000-8000-000000003017'::uuid, 'F1-04'),
    ('00000000-0000-4000-8000-000000004014'::uuid, 'LUM-0014', 'MAINTENANCE', '00000000-0000-4000-8000-000000003005'::uuid, 'T1-03'),
    ('00000000-0000-4000-8000-000000004015'::uuid, 'LUM-0015', 'UNAVAILABLE', '00000000-0000-4000-8000-000000003012'::uuid, 'C1-02')
ON CONFLICT (copy_code) DO UPDATE SET
    status         = EXCLUDED.status,
    book_id        = EXCLUDED.book_id,
    shelf_location = EXCLUDED.shelf_location;

-- ----------------------------------------------------------------------------
-- Loans (10): cobre todos os estados visiveis na UI
-- ----------------------------------------------------------------------------
INSERT INTO loan (id, borrowed_at, due_at, returned_at, status, reader_id, book_copy_id, renewal_count, penalty_code)
VALUES
    -- ACTIVE com prazos diversos (D-10, D-5, D-2, D+1)
    ('00000000-0000-4000-8000-000000005001'::uuid, now() - INTERVAL '4 days',  now() + INTERVAL '10 days', NULL, 'ACTIVE',    '00000000-0000-4000-8000-000000002401'::uuid, '00000000-0000-4000-8000-000000004002'::uuid, 0, NULL),
    ('00000000-0000-4000-8000-000000005003'::uuid, now() - INTERVAL '9 days',  now() + INTERVAL '5 days',  NULL, 'ACTIVE',    '00000000-0000-4000-8000-000000002404'::uuid, '00000000-0000-4000-8000-000000004009'::uuid, 0, NULL),
    ('00000000-0000-4000-8000-000000005004'::uuid, now() - INTERVAL '12 days', now() + INTERVAL '2 days',  NULL, 'ACTIVE',    '00000000-0000-4000-8000-000000002405'::uuid, '00000000-0000-4000-8000-000000004010'::uuid, 1, NULL),
    ('00000000-0000-4000-8000-000000005005'::uuid, now() - INTERVAL '15 days', now() - INTERVAL '1 days',  NULL, 'ACTIVE',    '00000000-0000-4000-8000-000000002406'::uuid, '00000000-0000-4000-8000-000000004011'::uuid, 0, NULL),
    ('00000000-0000-4000-8000-000000005006'::uuid, now() - INTERVAL '21 days', now() - INTERVAL '7 days',  now() - INTERVAL '5 days', 'COMPLETED', '00000000-0000-4000-8000-000000002403'::uuid, '00000000-0000-4000-8000-000000004012'::uuid, 1, NULL),
    -- OVERDUE: emprestimo com mais de 8 dias de atraso
    ('00000000-0000-4000-8000-000000005007'::uuid, now() - INTERVAL '30 days', now() - INTERVAL '8 days',  NULL, 'OVERDUE',   '00000000-0000-4000-8000-000000002408'::uuid, '00000000-0000-4000-8000-000000004013'::uuid, 0, 'SUSPENSION'),
    -- COMPLETED (5)
    ('00000000-0000-4000-8000-000000005002'::uuid, now() - INTERVAL '35 days', now() - INTERVAL '21 days', now() - INTERVAL '22 days', 'COMPLETED', '00000000-0000-4000-8000-000000002402'::uuid, '00000000-0000-4000-8000-000000004003'::uuid, 1, NULL),
    ('00000000-0000-4000-8000-000000005008'::uuid, now() - INTERVAL '90 days', now() - INTERVAL '76 days', now() - INTERVAL '78 days', 'COMPLETED', '00000000-0000-4000-8000-000000002401'::uuid, '00000000-0000-4000-8000-000000004001'::uuid, 0, NULL),
    ('00000000-0000-4000-8000-000000005009'::uuid, now() - INTERVAL '60 days', now() - INTERVAL '46 days', now() - INTERVAL '45 days', 'COMPLETED', '00000000-0000-4000-8000-000000002403'::uuid, '00000000-0000-4000-8000-000000004004'::uuid, 2, NULL),
    ('00000000-0000-4000-8000-000000005010'::uuid, now() - INTERVAL '45 days', now() - INTERVAL '31 days', now() - INTERVAL '20 days', 'COMPLETED', '00000000-0000-4000-8000-000000002404'::uuid, '00000000-0000-4000-8000-000000004005'::uuid, 0, 'WARNING')
ON CONFLICT (id) DO UPDATE SET
    borrowed_at   = EXCLUDED.borrowed_at,
    due_at        = EXCLUDED.due_at,
    returned_at   = EXCLUDED.returned_at,
    status        = EXCLUDED.status,
    reader_id    = EXCLUDED.reader_id,
    book_copy_id  = EXCLUDED.book_copy_id,
    renewal_count = EXCLUDED.renewal_count,
    penalty_code  = EXCLUDED.penalty_code;

-- ----------------------------------------------------------------------------
-- Loan requests (6)
-- ----------------------------------------------------------------------------
INSERT INTO loan_request (id, reader_id, book_copy_id, requested_at, status, note)
VALUES
    ('00000000-0000-4000-8000-000000006001'::uuid, '00000000-0000-4000-8000-000000002403'::uuid, '00000000-0000-4000-8000-000000004004'::uuid, now() - INTERVAL '1 day',  'PENDING',   'Solicitado via seed demo'),
    ('00000000-0000-4000-8000-000000006002'::uuid, '00000000-0000-4000-8000-000000002405'::uuid, '00000000-0000-4000-8000-000000004006'::uuid, now() - INTERVAL '2 days', 'PENDING',   'Aguardando aprovacao'),
    ('00000000-0000-4000-8000-000000006003'::uuid, '00000000-0000-4000-8000-000000002401'::uuid, '00000000-0000-4000-8000-000000004001'::uuid, now() - INTERVAL '12 days','ACCEPTED',  'Aceito e convertido em emprestimo concluido'),
    ('00000000-0000-4000-8000-000000006004'::uuid, '00000000-0000-4000-8000-000000002404'::uuid, '00000000-0000-4000-8000-000000004005'::uuid, now() - INTERVAL '20 days','ACCEPTED',  'Solicitacao aceita historica'),
    ('00000000-0000-4000-8000-000000006005'::uuid, '00000000-0000-4000-8000-000000002406'::uuid, '00000000-0000-4000-8000-000000004007'::uuid, now() - INTERVAL '3 days', 'REJECTED',  'Justificativa indisponivel no horario'),
    ('00000000-0000-4000-8000-000000006006'::uuid, '00000000-0000-4000-8000-000000002402'::uuid, '00000000-0000-4000-8000-000000004008'::uuid, now() - INTERVAL '4 days', 'CANCELLED', 'Cancelado pelo leitor')
ON CONFLICT (id) DO UPDATE SET
    reader_id   = EXCLUDED.reader_id,
    book_copy_id = EXCLUDED.book_copy_id,
    requested_at = EXCLUDED.requested_at,
    status       = EXCLUDED.status,
    note         = EXCLUDED.note;

-- ----------------------------------------------------------------------------
-- Reservations (5)
-- ----------------------------------------------------------------------------
INSERT INTO reservation (id, reader_id, book_id, status, queue_position, expires_at, notified_at)
VALUES
    ('00000000-0000-4000-8000-000000007001'::uuid, '00000000-0000-4000-8000-000000002402'::uuid, '00000000-0000-4000-8000-000000003001'::uuid, 'WAITING',   1, NULL,                          NULL),
    ('00000000-0000-4000-8000-000000007002'::uuid, '00000000-0000-4000-8000-000000002405'::uuid, '00000000-0000-4000-8000-000000003001'::uuid, 'WAITING',   2, NULL,                          NULL),
    ('00000000-0000-4000-8000-000000007003'::uuid, '00000000-0000-4000-8000-000000002406'::uuid, '00000000-0000-4000-8000-000000003020'::uuid, 'READY',     1, now() + INTERVAL '2 days',     now() - INTERVAL '6 hours'),
    ('00000000-0000-4000-8000-000000007004'::uuid, '00000000-0000-4000-8000-000000002401'::uuid, '00000000-0000-4000-8000-000000003019'::uuid, 'FULFILLED', 1, now() - INTERVAL '5 days',     now() - INTERVAL '8 days'),
    ('00000000-0000-4000-8000-000000007005'::uuid, '00000000-0000-4000-8000-000000002404'::uuid, '00000000-0000-4000-8000-000000003017'::uuid, 'EXPIRED',   1, now() - INTERVAL '1 day',      now() - INTERVAL '5 days')
ON CONFLICT (id) DO UPDATE SET
    reader_id     = EXCLUDED.reader_id,
    book_id        = EXCLUDED.book_id,
    status         = EXCLUDED.status,
    queue_position = EXCLUDED.queue_position,
    expires_at     = EXCLUDED.expires_at,
    notified_at    = EXCLUDED.notified_at;

-- ----------------------------------------------------------------------------
-- App content (14) - mural do app: WORK/TCC (migrados) + comunicados + anexos.
-- Cobre todos os tipos, estados (publicado/oculto/agendado/expirado), destaque
-- (pinned + display_order) e segmentacao (ALL / COURSE / MODULE / SHIFT).
-- ----------------------------------------------------------------------------
INSERT INTO app_content (
    id, content_type, title, body, authors, advisors, completion_year, completion_semester,
    cover_url, file_url, external_url, is_published, is_pinned, display_order,
    audience_scope, course_id, academic_module_id, study_shift_id, publish_start_at, publish_end_at
)
VALUES
    -- Trabalhos/TCC (migrados do thesis)
    ('00000000-0000-4000-8000-000000008001'::uuid, 'WORK', 'Sistema de Catalogo Digital para Biblioteca Escolar', NULL, 'Ana Beatriz Lima; Mariana Oliveira Santos', 'Prof. Joao Pereira',  2025, '2', 'https://covers.openlibrary.org/b/id/10523338-L.jpg', 'https://www.africau.edu/images/default/sample.pdf', 'https://example.com/lumilivre/tcc-demo-1', TRUE, FALSE, 10, 'ALL', NULL, NULL, NULL, NULL, NULL),
    ('00000000-0000-4000-8000-000000008002'::uuid, 'WORK', 'Gestao de Estoque para Pequenas Bibliotecas Escolares', NULL, 'Carlos Henrique Souza', 'Profa. Beatriz Nunes', 2024, '2', 'https://covers.openlibrary.org/b/id/240727-L.jpg', 'https://www.africau.edu/images/default/sample.pdf', NULL, TRUE, FALSE, 11, 'ALL', NULL, NULL, NULL, NULL, NULL),
    ('00000000-0000-4000-8000-000000008003'::uuid, 'WORK', 'Automacao de Importacao de Acervos via Planilhas', NULL, 'Pedro Henrique Costa; Lucas Pereira Cardoso', 'Prof. Lucio Almeida', 2025, '1', 'https://covers.openlibrary.org/b/id/240727-L.jpg', 'https://www.africau.edu/images/default/sample.pdf', NULL, TRUE, FALSE, 12, 'ALL', NULL, NULL, NULL, NULL, NULL),
    -- Comunicados
    ('00000000-0000-4000-8000-000000008010'::uuid, 'ANNOUNCEMENT', 'Bem-vindos ao novo semestre!', 'A biblioteca esta de portas abertas. Confira o novo horario de funcionamento e os lancamentos do acervo.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, TRUE, TRUE, 0, 'ALL', NULL, NULL, NULL, NULL, NULL),
    ('00000000-0000-4000-8000-000000008011'::uuid, 'ANNOUNCEMENT', 'Manutencao no sabado', 'No proximo sabado a biblioteca estara fechada para manutencao. Devolucoes podem ser feitas na segunda.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, TRUE, FALSE, 1, 'ALL', NULL, NULL, NULL, NULL, NULL),
    ('00000000-0000-4000-8000-000000008012'::uuid, 'ANNOUNCEMENT', 'Semana de provas: horario estendido (agendado)', 'Durante a semana de provas a biblioteca abrira mais cedo. Este comunicado ainda esta agendado.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, TRUE, FALSE, 2, 'ALL', NULL, NULL, NULL, now() + INTERVAL '7 days', NULL),
    ('00000000-0000-4000-8000-000000008013'::uuid, 'ANNOUNCEMENT', 'Campanha de doacao (encerrada)', 'A campanha de doacao de livros foi encerrada. Obrigado a todos que participaram!', NULL, NULL, NULL, NULL, NULL, NULL, NULL, TRUE, FALSE, 3, 'ALL', NULL, NULL, NULL, now() - INTERVAL '30 days', now() - INTERVAL '2 days'),
    ('00000000-0000-4000-8000-000000008014'::uuid, 'ANNOUNCEMENT', 'Reuniao do curso de Desenvolvimento de Sistemas', 'Alunos de Desenvolvimento de Sistemas: reuniao sobre o projeto integrador na proxima quinta.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, TRUE, FALSE, 4, 'COURSE', (SELECT id FROM course WHERE name = 'Desenvolvimento de Sistemas'), NULL, NULL, NULL, NULL),
    ('00000000-0000-4000-8000-000000008015'::uuid, 'ANNOUNCEMENT', 'Atividade do Modulo 2', 'Entrega da atividade avaliativa do Modulo 2 ate sexta-feira.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, TRUE, FALSE, 5, 'MODULE', NULL, (SELECT id FROM academic_module WHERE name = 'Módulo 2'), NULL, NULL, NULL),
    ('00000000-0000-4000-8000-000000008016'::uuid, 'ANNOUNCEMENT', 'Aviso para o turno Noturno', 'A saida antecipada do turno noturno esta liberada nesta semana.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, TRUE, FALSE, 6, 'SHIFT', NULL, NULL, (SELECT id FROM study_shift WHERE name = 'Noturno'), NULL, NULL),
    ('00000000-0000-4000-8000-000000008019'::uuid, 'ANNOUNCEMENT', 'Rascunho interno (nao publicado)', 'Este comunicado esta oculto (is_published = FALSE) e nao deve aparecer no mural.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, FALSE, FALSE, 7, 'ALL', NULL, NULL, NULL, NULL, NULL),
    -- Anexos (documentos)
    ('00000000-0000-4000-8000-000000008017'::uuid, 'ATTACHMENT', 'Regulamento da biblioteca', 'Baixe o regulamento atualizado de uso da biblioteca.', NULL, NULL, NULL, NULL, NULL, 'https://www.africau.edu/images/default/sample.pdf', NULL, TRUE, FALSE, 8, 'ALL', NULL, NULL, NULL, NULL, NULL),
    ('00000000-0000-4000-8000-000000008018'::uuid, 'ATTACHMENT', 'Calendario academico 2025', 'Documento com as datas importantes do ano letivo.', NULL, NULL, NULL, NULL, NULL, 'https://www.africau.edu/images/default/sample.pdf', NULL, TRUE, FALSE, 9, 'ALL', NULL, NULL, NULL, NULL, NULL),
    -- Trabalho segmentado por curso
    ('00000000-0000-4000-8000-000000008020'::uuid, 'WORK', 'Primeiros Socorros na Escola', 'Trabalho de conclusao sobre protocolos de primeiros socorros.', 'Juliana Ferreira Rocha', 'Profa. Sandra Melo', 2024, '1', 'https://covers.openlibrary.org/b/id/10523338-L.jpg', 'https://www.africau.edu/images/default/sample.pdf', NULL, TRUE, FALSE, 13, 'COURSE', (SELECT id FROM course WHERE name = 'Técnico em Enfermagem'), NULL, NULL, NULL, NULL)
ON CONFLICT (id) DO UPDATE SET
    content_type        = EXCLUDED.content_type,
    title               = EXCLUDED.title,
    body                = EXCLUDED.body,
    authors             = EXCLUDED.authors,
    advisors            = EXCLUDED.advisors,
    completion_year     = EXCLUDED.completion_year,
    completion_semester = EXCLUDED.completion_semester,
    cover_url           = EXCLUDED.cover_url,
    file_url            = EXCLUDED.file_url,
    external_url        = EXCLUDED.external_url,
    is_published        = EXCLUDED.is_published,
    is_pinned           = EXCLUDED.is_pinned,
    display_order       = EXCLUDED.display_order,
    audience_scope      = EXCLUDED.audience_scope,
    course_id           = EXCLUDED.course_id,
    academic_module_id  = EXCLUDED.academic_module_id,
    study_shift_id      = EXCLUDED.study_shift_id,
    publish_start_at    = EXCLUDED.publish_start_at,
    publish_end_at      = EXCLUDED.publish_end_at;

-- ----------------------------------------------------------------------------
-- Audit log (3) - tabela append-only sem PK natural; idempotencia via DELETE+INSERT
-- ----------------------------------------------------------------------------
DELETE FROM audit_log WHERE target_id LIKE 'demo-%';

INSERT INTO audit_log (actor, actor_role, target_id, action, result, error_message, occurred_at)
VALUES
    ('admin',     'ADMIN',     'demo-loan-001',    'LOAN_CREATED',   'SUCCESS', NULL,                              now() - INTERVAL '6 hours'),
    ('librarian', 'LIBRARIAN', 'demo-request-002', 'REQUEST_PROCESSED','FAILURE','Leitor bloqueado por penalidade',  now() - INTERVAL '3 hours'),
    ('studant',     'READER',   'demo-reader-2401','READER_VIEW',   'DENIED',  'Tentativa de acessar outro leitor',now() - INTERVAL '30 minutes');

-- ----------------------------------------------------------------------------
-- Access log (12) - trilha de acessos (WS-07): LOGIN / LOGIN_FAILED / ACCESS_DENIED,
-- canais WEB/APP, IPs e user-agents variados. Idempotente via marcador correlation_id.
-- ----------------------------------------------------------------------------
DELETE FROM access_log WHERE correlation_id = 'demo-seed';

INSERT INTO access_log (actor, actor_role, event, channel, result, ip_address, user_agent, correlation_id, error_message, occurred_at)
VALUES
    ('admin',     'ROLE_ADMIN',     'LOGIN',         'WEB', 'SUCCESS', '187.12.44.10',  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125', 'demo-seed', NULL,                  now() - INTERVAL '2 hours'),
    ('librarian', 'ROLE_LIBRARIAN', 'LOGIN',         'WEB', 'SUCCESS', '187.12.44.11',  'Mozilla/5.0 (Macintosh) Safari/17',                    'demo-seed', NULL,                  now() - INTERVAL '5 hours'),
    ('2024001',   'ROLE_READER',    'LOGIN',         'APP', 'SUCCESS', '200.150.10.22', 'Dart/3.9 (dart:io) lumilivre',                         'demo-seed', NULL,                  now() - INTERVAL '1 hours'),
    ('2024002',   'ROLE_READER',    'LOGIN',         'APP', 'SUCCESS', '200.150.10.23', 'Dart/3.9 (dart:io) lumilivre',                         'demo-seed', NULL,                  now() - INTERVAL '3 hours'),
    ('9999999',   'ANONYMOUS',      'LOGIN_FAILED',  'APP', 'FAILURE', '45.10.200.5',   'Dart/3.9 (dart:io) lumilivre',                         'demo-seed', 'invalid-credentials', now() - INTERVAL '20 minutes'),
    ('admin',     'ANONYMOUS',      'LOGIN_FAILED',  'WEB', 'FAILURE', '45.10.200.6',   'Mozilla/5.0 (X11; Linux) Firefox/126',                 'demo-seed', 'invalid-credentials', now() - INTERVAL '18 minutes'),
    ('2024003',   'ROLE_READER',    'ACCESS_DENIED', 'APP', 'DENIED',  '200.150.10.24', 'Dart/3.9 (dart:io) lumilivre',                         'demo-seed', 'Access is denied',    now() - INTERVAL '40 minutes'),
    ('2024001',   'ROLE_READER',    'LOGIN',         'APP', 'SUCCESS', '200.150.10.22', 'Dart/3.9 (dart:io) lumilivre',                         'demo-seed', NULL,                  now() - INTERVAL '2 days'),
    ('librarian', 'ROLE_LIBRARIAN', 'LOGIN',         'WEB', 'SUCCESS', '187.12.44.11',  'Mozilla/5.0 (Windows NT 10.0) Edge/125',               'demo-seed', NULL,                  now() - INTERVAL '3 days'),
    ('admin',     'ROLE_ADMIN',     'LOGIN',         'WEB', 'SUCCESS', '187.12.44.10',  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125', 'demo-seed', NULL,                  now() - INTERVAL '6 days'),
    ('8888888',   'ANONYMOUS',      'LOGIN_FAILED',  'WEB', 'FAILURE', '103.55.2.9',    'Mozilla/5.0 (Windows NT 10.0) Chrome/124',             'demo-seed', 'invalid-credentials', now() - INTERVAL '7 days'),
    ('2024004',   'ROLE_READER',    'LOGIN',         'APP', 'SUCCESS', '200.150.10.25', 'Dart/3.9 (dart:io) lumilivre',                         'demo-seed', NULL,                  now() - INTERVAL '10 days');

-- ----------------------------------------------------------------------------
-- Outbox events (2) - historico de notificacoes SENT
-- ----------------------------------------------------------------------------
DELETE FROM outbox_event WHERE subject LIKE 'Demo:%';

INSERT INTO outbox_event (event_type, recipient_email, subject, body, status, retry_count, processed_at)
VALUES
    ('LOAN_CREATED',     'ana.lima@example.com',    'Demo: Empréstimo registrado',
        'Empréstimo demo enviado pelo seed.',                'SENT', 0, now() - INTERVAL '5 hours'),
    ('REQUEST_ACCEPTED', 'mariana.santos@example.com','Demo: Solicitação aceita',
        'Solicitação demo aceita; notificação enviada.',     'SENT', 0, now() - INTERVAL '2 hours');

-- ============================================================================
-- VOLUME EXPANSION (WS-06)
-- Dados gerados deterministicamente (generate_series, sem random()) para dar
-- evidencia visual a todas as features: paginacao real, dashboard populado
-- (mv_loans_by_month com ~12 meses), penalidades, filas de reserva, etc.
-- Totais apos expansao: reader=50, book=100, book_copy=150, loan=60,
-- loan_request=15, reservation=15, access_log=50, audit_log=15, outbox=5.
-- ============================================================================

-- Readers gerados (2024009..2024050) --------------------------------------------------
INSERT INTO reader (
    id, registration_number, full_name, avatar_url, cpf, birth_date, phone_number, email,
    course_id, academic_module_id, study_shift_id,
    postal_code, street, district, city, state_code, street_number,
    penalty_code, penalty_expires_at
)
SELECT
    ('00000000-0000-4000-8000-00000000' || lpad((2400 + g.i)::text, 4, '0'))::uuid,
    '20240' || lpad(g.i::text, 2, '0'),
    (ARRAY['Miguel','Sofia','Arthur','Alice','Heitor','Laura','Bernardo','Valentina','Davi','Helena','Gabriel','Julia','Rafael','Isadora'])[1 + ((g.i * 5) % 14)]
        || ' ' || (ARRAY['Silva','Santos','Oliveira','Souza','Costa','Rodrigues','Almeida','Nascimento','Lima','Araujo','Pereira','Carvalho'])[1 + ((g.i * 3) % 12)]
        || ' ' || (ARRAY['Silva','Santos','Oliveira','Souza','Costa','Rodrigues','Almeida','Nascimento','Lima','Araujo','Pereira','Carvalho'])[1 + ((g.i * 7 + 4) % 12)],
    CASE WHEN g.i % 3 = 0 THEN NULL ELSE 'https://i.pravatar.cc/150?img=' || (1 + (g.i % 70)) END,
    CASE WHEN g.i % 7 = 0 THEN NULL ELSE (52210000000 + g.i * 37)::text END,
    DATE '2004-01-01' + ((g.i * 89) % 1400),
    '119' || lpad((90000000 + g.i * 13)::text, 8, '0'),
    CASE WHEN g.i % 6 = 0 THEN NULL ELSE 'leitor' || (2400 + g.i) || '@example.com' END,
    ref.cids[1 + (g.i % cardinality(ref.cids))],
    ref.mids[1 + (g.i % cardinality(ref.mids))],
    ref.sids[1 + (g.i % cardinality(ref.sids))],
    '01310100', 'Avenida Demo', 'Centro', 'Sao Paulo', 'SP', g.i,
    CASE WHEN g.i % 11 = 0 THEN 'BLOCK'
         WHEN g.i % 13 = 0 THEN 'WARNING'
         WHEN g.i % 17 = 0 THEN 'WARNING'
         ELSE NULL END,
    CASE WHEN g.i % 11 = 0 THEN now() + ((1 + g.i % 5) || ' days')::interval
         WHEN g.i % 13 = 0 THEN now() + INTERVAL '2 days'
         WHEN g.i % 17 = 0 THEN now() - INTERVAL '3 days'
         ELSE NULL END
FROM generate_series(9, 50) AS g(i)
CROSS JOIN (
    SELECT (SELECT array_agg(id ORDER BY id) FROM course)          AS cids,
           (SELECT array_agg(id ORDER BY id) FROM academic_module) AS mids,
           (SELECT array_agg(id ORDER BY id) FROM study_shift)     AS sids
) AS ref
ON CONFLICT (registration_number) DO UPDATE SET
    full_name          = EXCLUDED.full_name,
    avatar_url         = EXCLUDED.avatar_url,
    cpf                = EXCLUDED.cpf,
    birth_date         = EXCLUDED.birth_date,
    phone_number       = EXCLUDED.phone_number,
    email              = EXCLUDED.email,
    course_id          = EXCLUDED.course_id,
    academic_module_id = EXCLUDED.academic_module_id,
    study_shift_id     = EXCLUDED.study_shift_id,
    penalty_code       = EXCLUDED.penalty_code,
    penalty_expires_at = EXCLUDED.penalty_expires_at;

-- Staff extra (demonstra 1o acesso de staff: must_change_password) -------------------
-- Senha de librarian2 = 'librarian' (mesmo hash do librarian).
INSERT INTO app_user (id, email, password_hash, role, reader_id, preferred_locale,
                      must_change_password, guided_tour_completed)
VALUES
    ('00000000-0000-4000-8000-000000001004'::uuid, 'librarian2',
        '$2b$12$PGsQqsJvPgZkTgh3cGJ7qu6744vKKJ7PL0QRg12LY5p38be7s5jBe',
        'LIBRARIAN', NULL, 'pt-BR', TRUE, FALSE)
ON CONFLICT (email) DO UPDATE SET
    password_hash         = EXCLUDED.password_hash,
    must_change_password  = EXCLUDED.must_change_password,
    guided_tour_completed = EXCLUDED.guided_tour_completed;

-- Books gerados (3031..3100) ----------------------------------------------------------
INSERT INTO book (
    id, isbn, title, publication_date, page_count, dewey_code, publisher,
    age_rating, edition, volume, synopsis, author, cover_type, cover_url, rating
)
SELECT
    ('00000000-0000-4000-8000-00000000' || lpad((3000 + g.i)::text, 4, '0'))::uuid,
    CASE WHEN g.i % 9 = 0 THEN NULL ELSE '97865' || (20000000 + g.i)::text END,
    (ARRAY['Fundamentos de','Introducao a','Manual de','Guia Pratico de','Historias de','Atlas de'])[1 + (g.i % 6)]
        || ' ' || (ARRAY['Banco de Dados','Redes de Computadores','Quimica Organica','Historia do Brasil','Matematica Aplicada','Biologia Celular','Educacao Fisica','Artes Visuais','Literatura Brasileira','Fisica Moderna','Geografia Humana','Filosofia','Sociologia','Programacao Web'])[1 + (g.i % 14)]
        || CASE WHEN g.i >= 73 THEN ' - Vol. 2' ELSE '' END,
    DATE '1980-01-01' + (g.i * 100),
    120 + ((g.i * 13) % 400),
    (ARRAY['005','869','823','813','843','909','940','109'])[1 + (g.i % 8)],
    (ARRAY['LumiLivre Educacional','Editora Ficticia Escola','Editora Horizonte Demo','Saber & Cia Demo','Atlas Editorial Demo'])[1 + (g.i % 5)],
    (ARRAY['CHILDREN','MIDDLE_GRADE','TEEN','ADULT','GENERAL'])[1 + (g.i % 5)],
    '1a edicao demo',
    NULL,
    'Material didatico de demonstracao para a estante de '
        || (ARRAY['Banco de Dados','Redes de Computadores','Quimica Organica','Historia do Brasil','Matematica Aplicada','Biologia Celular','Educacao Fisica','Artes Visuais','Literatura Brasileira','Fisica Moderna','Geografia Humana','Filosofia','Sociologia','Programacao Web'])[1 + (g.i % 14)] || '.',
    (ARRAY['Ana Ribeiro','Bruno Teixeira','Carla Mendes','Diego Fonseca','Elisa Prado','Fabio Nogueira','Gisele Ramos','Hugo Siqueira','Iris Camargo','Joao Batista'])[1 + ((g.i * 3) % 10)],
    (ARRAY['HARDCOVER','SOFTCOVER','BOARD_BOOK','DUST_JACKET','SPIRAL','PAPERBACK'])[1 + (g.i % 6)],
    CASE WHEN g.i % 9 = 0 THEN NULL ELSE 'https://covers.openlibrary.org/b/id/' || (8000000 + g.i * 11) || '-L.jpg' END,
    3.0 + ((g.i % 20)::double precision / 10.0)
FROM generate_series(31, 100) AS g(i)
ON CONFLICT (id) DO UPDATE SET
    isbn       = EXCLUDED.isbn,
    title      = EXCLUDED.title,
    author     = EXCLUDED.author,
    publisher  = EXCLUDED.publisher,
    cover_url  = EXCLUDED.cover_url,
    rating     = EXCLUDED.rating;

-- Generos dos books gerados (1 a 3 generos por livro) ---------------------------------
INSERT INTO book_genre (book_id, genre_id)
SELECT ('00000000-0000-4000-8000-00000000' || lpad((3000 + g.i)::text, 4, '0'))::uuid,
       ref.gids[1 + (g.i % cardinality(ref.gids))]
FROM generate_series(31, 100) AS g(i)
CROSS JOIN (SELECT (SELECT array_agg(id ORDER BY id) FROM genre) AS gids) AS ref
ON CONFLICT (book_id, genre_id) DO NOTHING;

INSERT INTO book_genre (book_id, genre_id)
SELECT ('00000000-0000-4000-8000-00000000' || lpad((3000 + g.i)::text, 4, '0'))::uuid,
       ref.gids[1 + ((g.i * 3 + 1) % cardinality(ref.gids))]
FROM generate_series(31, 100) AS g(i)
CROSS JOIN (SELECT (SELECT array_agg(id ORDER BY id) FROM genre) AS gids) AS ref
WHERE g.i % 2 = 0
ON CONFLICT (book_id, genre_id) DO NOTHING;

INSERT INTO book_genre (book_id, genre_id)
SELECT ('00000000-0000-4000-8000-00000000' || lpad((3000 + g.i)::text, 4, '0'))::uuid,
       ref.gids[1 + ((g.i * 5 + 2) % cardinality(ref.gids))]
FROM generate_series(31, 100) AS g(i)
CROSS JOIN (SELECT (SELECT array_agg(id ORDER BY id) FROM genre) AS gids) AS ref
WHERE g.i % 3 = 0
ON CONFLICT (book_id, genre_id) DO NOTHING;

-- Exemplares gerados (4016..4150) ------------------------------------------------------
-- 4016-4040 BORROWED (casam com loans ativos/atrasados abaixo);
-- 4041-4130 AVAILABLE; 4131-4140 MAINTENANCE; 4141-4150 UNAVAILABLE.
INSERT INTO book_copy (id, copy_code, status, book_id, shelf_location)
SELECT
    ('00000000-0000-4000-8000-00000000' || lpad((4000 + g.i)::text, 4, '0'))::uuid,
    'LUM-' || lpad(g.i::text, 4, '0'),
    CASE WHEN g.i BETWEEN 16 AND 40 THEN 'BORROWED'
         WHEN g.i BETWEEN 131 AND 140 THEN 'MAINTENANCE'
         WHEN g.i BETWEEN 141 AND 150 THEN 'UNAVAILABLE'
         ELSE 'AVAILABLE' END,
    ('00000000-0000-4000-8000-00000000' || lpad((3000 + 1 + ((g.i * 7) % 100))::text, 4, '0'))::uuid,
    chr(65 + (g.i % 6)) || (1 + (g.i % 4)) || '-' || lpad((1 + ((g.i * 3) % 20))::text, 2, '0')
FROM generate_series(16, 150) AS g(i)
ON CONFLICT (copy_code) DO UPDATE SET
    status         = EXCLUDED.status,
    book_id        = EXCLUDED.book_id,
    shelf_location = EXCLUDED.shelf_location;

-- Loans gerados (5011..5060) -----------------------------------------------------------
-- 5011-5030 ACTIVE (vencimentos 1..13 dias); 5031-5035 OVERDUE; 5036-5060 COMPLETED
-- espalhados em ~12 meses (alimenta mv_loans_by_month -> WS-04).
INSERT INTO loan (id, borrowed_at, due_at, returned_at, status, reader_id, book_copy_id, renewal_count, penalty_code)
SELECT
    ('00000000-0000-4000-8000-00000000' || lpad((5000 + g.i)::text, 4, '0'))::uuid,
    CASE WHEN g.i <= 30 THEN now() - ((1 + (g.i % 13)) || ' days')::interval
         WHEN g.i <= 35 THEN now() - ((20 + g.i) || ' days')::interval
         ELSE now() - (((g.i - 35) * 14) || ' days')::interval END,
    CASE WHEN g.i <= 30 THEN now() - ((1 + (g.i % 13)) || ' days')::interval + INTERVAL '14 days'
         WHEN g.i <= 35 THEN now() - ((20 + g.i) || ' days')::interval + INTERVAL '14 days'
         ELSE now() - (((g.i - 35) * 14) || ' days')::interval + INTERVAL '14 days' END,
    CASE WHEN g.i <= 35 THEN NULL
         WHEN g.i % 5 = 0 THEN now() - (((g.i - 35) * 14) || ' days')::interval + INTERVAL '17 days'
         ELSE now() - (((g.i - 35) * 14) || ' days')::interval + INTERVAL '12 days' END,
    CASE WHEN g.i <= 30 THEN 'ACTIVE' WHEN g.i <= 35 THEN 'OVERDUE' ELSE 'COMPLETED' END,
    ('00000000-0000-4000-8000-00000000' || lpad((2400 + 9 + ((g.i * 11) % 42))::text, 4, '0'))::uuid,
    ('00000000-0000-4000-8000-00000000' || lpad((4000 + g.i + 5)::text, 4, '0'))::uuid,
    g.i % 3,
    CASE WHEN g.i BETWEEN 31 AND 35 AND g.i % 2 = 1 THEN 'SUSPENSION' ELSE NULL END
FROM generate_series(11, 60) AS g(i)
ON CONFLICT (id) DO UPDATE SET
    borrowed_at   = EXCLUDED.borrowed_at,
    due_at        = EXCLUDED.due_at,
    returned_at   = EXCLUDED.returned_at,
    status        = EXCLUDED.status,
    reader_id     = EXCLUDED.reader_id,
    book_copy_id  = EXCLUDED.book_copy_id,
    renewal_count = EXCLUDED.renewal_count,
    penalty_code  = EXCLUDED.penalty_code;

-- Loan requests gerados (6007..6015) ---------------------------------------------------
INSERT INTO loan_request (id, reader_id, book_copy_id, requested_at, status, note)
SELECT
    ('00000000-0000-4000-8000-00000000' || lpad((6000 + g.i)::text, 4, '0'))::uuid,
    ('00000000-0000-4000-8000-00000000' || lpad((2400 + 9 + ((g.i * 5) % 42))::text, 4, '0'))::uuid,
    ('00000000-0000-4000-8000-00000000' || lpad((4000 + 66 + g.i)::text, 4, '0'))::uuid,
    now() - (g.i || ' days')::interval,
    (ARRAY['PENDING','ACCEPTED','REJECTED','CANCELLED'])[1 + (g.i % 4)],
    'Solicitacao demo gerada'
FROM generate_series(7, 15) AS g(i)
ON CONFLICT (id) DO UPDATE SET
    reader_id    = EXCLUDED.reader_id,
    book_copy_id = EXCLUDED.book_copy_id,
    requested_at = EXCLUDED.requested_at,
    status       = EXCLUDED.status;

-- Reservations geradas (7006..7015): fila FIFO no mesmo livro + demais estados ---------
INSERT INTO reservation (id, reader_id, book_id, status, queue_position, expires_at, notified_at)
VALUES
    ('00000000-0000-4000-8000-000000007006'::uuid, '00000000-0000-4000-8000-000000002409'::uuid, '00000000-0000-4000-8000-000000003031'::uuid, 'WAITING',   1, NULL,                      NULL),
    ('00000000-0000-4000-8000-000000007007'::uuid, '00000000-0000-4000-8000-000000002410'::uuid, '00000000-0000-4000-8000-000000003031'::uuid, 'WAITING',   2, NULL,                      NULL),
    ('00000000-0000-4000-8000-000000007008'::uuid, '00000000-0000-4000-8000-000000002411'::uuid, '00000000-0000-4000-8000-000000003031'::uuid, 'WAITING',   3, NULL,                      NULL),
    ('00000000-0000-4000-8000-000000007009'::uuid, '00000000-0000-4000-8000-000000002412'::uuid, '00000000-0000-4000-8000-000000003032'::uuid, 'READY',     1, now() + INTERVAL '2 days', now() - INTERVAL '3 hours'),
    ('00000000-0000-4000-8000-000000007010'::uuid, '00000000-0000-4000-8000-000000002413'::uuid, '00000000-0000-4000-8000-000000003033'::uuid, 'READY',     1, now() + INTERVAL '1 day',  now() - INTERVAL '1 hour'),
    ('00000000-0000-4000-8000-000000007011'::uuid, '00000000-0000-4000-8000-000000002414'::uuid, '00000000-0000-4000-8000-000000003034'::uuid, 'FULFILLED', 1, now() - INTERVAL '3 days', now() - INTERVAL '5 days'),
    ('00000000-0000-4000-8000-000000007012'::uuid, '00000000-0000-4000-8000-000000002415'::uuid, '00000000-0000-4000-8000-000000003035'::uuid, 'FULFILLED', 1, now() - INTERVAL '6 days', now() - INTERVAL '8 days'),
    ('00000000-0000-4000-8000-000000007013'::uuid, '00000000-0000-4000-8000-000000002416'::uuid, '00000000-0000-4000-8000-000000003036'::uuid, 'FULFILLED', 1, now() - INTERVAL '9 days', now() - INTERVAL '11 days'),
    ('00000000-0000-4000-8000-000000007014'::uuid, '00000000-0000-4000-8000-000000002417'::uuid, '00000000-0000-4000-8000-000000003037'::uuid, 'EXPIRED',   1, now() - INTERVAL '1 day',  now() - INTERVAL '4 days'),
    ('00000000-0000-4000-8000-000000007015'::uuid, '00000000-0000-4000-8000-000000002418'::uuid, '00000000-0000-4000-8000-000000003038'::uuid, 'CANCELLED', 1, NULL,                      now() - INTERVAL '2 days')
ON CONFLICT (id) DO UPDATE SET
    reader_id      = EXCLUDED.reader_id,
    book_id        = EXCLUDED.book_id,
    status         = EXCLUDED.status,
    queue_position = EXCLUDED.queue_position,
    expires_at     = EXCLUDED.expires_at,
    notified_at    = EXCLUDED.notified_at;

-- Audit log gerado (12) ----------------------------------------------------------------
-- O DELETE 'demo-%' da secao original ja limpou estes na re-execucao.
INSERT INTO audit_log (actor, actor_role, target_id, action, result, error_message, ip_address, occurred_at)
VALUES
    ('admin',     'ROLE_ADMIN',     'demo-gen-001', 'READER_CREATED',      'SUCCESS', NULL,                          '187.12.44.10', now() - INTERVAL '1 day'),
    ('admin',     'ROLE_ADMIN',     'demo-gen-002', 'READER_CREATED',      'SUCCESS', NULL,                          '187.12.44.10', now() - INTERVAL '2 days'),
    ('librarian', 'ROLE_LIBRARIAN', 'demo-gen-003', 'READER_UPDATED',      'SUCCESS', NULL,                          '187.12.44.11', now() - INTERVAL '3 days'),
    ('librarian', 'ROLE_LIBRARIAN', 'demo-gen-004', 'BOOK_CREATED',        'SUCCESS', NULL,                          '187.12.44.11', now() - INTERVAL '4 days'),
    ('admin',     'ROLE_ADMIN',     'demo-gen-005', 'BOOK_CREATED',        'SUCCESS', NULL,                          '187.12.44.10', now() - INTERVAL '5 days'),
    ('librarian', 'ROLE_LIBRARIAN', 'demo-gen-006', 'BOOK_UPDATED',        'SUCCESS', NULL,                          '187.12.44.11', now() - INTERVAL '6 days'),
    ('admin',     'ROLE_ADMIN',     'demo-gen-007', 'SETTINGS_UPDATED',    'SUCCESS', NULL,                          '187.12.44.10', now() - INTERVAL '7 days'),
    ('admin',     'ROLE_ADMIN',     'demo-gen-008', 'APP_VERSION_UPDATED', 'SUCCESS', NULL,                          '187.12.44.10', now() - INTERVAL '8 days'),
    ('librarian', 'ROLE_LIBRARIAN', 'demo-gen-009', 'LOAN_CREATED',        'SUCCESS', NULL,                          '187.12.44.11', now() - INTERVAL '9 days'),
    ('librarian', 'ROLE_LIBRARIAN', 'demo-gen-010', 'REQUEST_PROCESSED',   'FAILURE', 'Exemplar indisponivel',       '187.12.44.11', now() - INTERVAL '10 days'),
    ('2024010',   'ROLE_READER',    'demo-gen-011', 'READER_VIEW',         'DENIED',  'Acesso a outro leitor negado','200.150.10.30', now() - INTERVAL '11 days'),
    ('admin',     'ROLE_ADMIN',     'demo-gen-012', 'SETTINGS_UPDATED',    'SUCCESS', NULL,                          '187.12.44.10', now() - INTERVAL '12 days');

-- Access log gerado (38) ----------------------------------------------------------------
DELETE FROM access_log WHERE correlation_id = 'demo-seed-gen';

INSERT INTO access_log (actor, actor_role, event, channel, result, ip_address, user_agent, correlation_id, error_message, occurred_at)
SELECT
    '20240' || lpad((9 + ((g.i * 3) % 42))::text, 2, '0'),
    CASE WHEN g.i % 13 = 0 THEN 'ROLE_READER' WHEN g.i % 8 = 0 THEN 'ANONYMOUS' ELSE 'ROLE_READER' END,
    CASE WHEN g.i % 13 = 0 THEN 'ACCESS_DENIED' WHEN g.i % 8 = 0 THEN 'LOGIN_FAILED' ELSE 'LOGIN' END,
    CASE WHEN g.i % 2 = 0 THEN 'APP' ELSE 'WEB' END,
    CASE WHEN g.i % 13 = 0 THEN 'DENIED' WHEN g.i % 8 = 0 THEN 'FAILURE' ELSE 'SUCCESS' END,
    '10.20.' || (g.i % 250) || '.' || ((g.i * 7) % 250),
    CASE WHEN g.i % 2 = 0 THEN 'Dart/3.9 (dart:io) lumilivre' ELSE 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125' END,
    'demo-seed-gen',
    CASE WHEN g.i % 8 = 0 THEN 'invalid-credentials' WHEN g.i % 13 = 0 THEN 'Access is denied' ELSE NULL END,
    now() - (g.i * 19 || ' hours')::interval
FROM generate_series(1, 38) AS g(i);

-- Outbox extra (3): PENDING / FAILED / SENT ---------------------------------------------
INSERT INTO outbox_event (event_type, recipient_email, subject, body, status, retry_count, processed_at, next_retry_at)
VALUES
    ('LOAN_DUE_SOON',   'leitor2412@example.com', 'Demo: Emprestimo vence em breve',
        'Lembrete demo de vencimento.',            'PENDING', 0, NULL,                        now() + INTERVAL '30 minutes'),
    ('LOAN_OVERDUE',    'leitor2415@example.com', 'Demo: Emprestimo em atraso',
        'Notificacao demo de atraso.',             'FAILED',  3, NULL,                        now() + INTERVAL '2 hours'),
    ('RESERVATION_READY','leitor2412@example.com','Demo: Reserva disponivel',
        'Sua reserva demo esta pronta para retirada.', 'SENT', 0, now() - INTERVAL '1 hour', NULL);

-- ----------------------------------------------------------------------------
-- Refresh materialized views (sem CONCURRENTLY: mv_dashboard_stats nao tem UNIQUE INDEX rowmaster)
-- ----------------------------------------------------------------------------
REFRESH MATERIALIZED VIEW mv_dashboard_stats;
REFRESH MATERIALIZED VIEW mv_top_books;
REFRESH MATERIALIZED VIEW mv_loans_by_month;

DO $$
DECLARE
    seed_table text;
BEGIN
    FOREACH seed_table IN ARRAY ARRAY[
        'reader',
        'library_settings',
        'app_user',
        'book',
        'book_genre',
        'book_copy',
        'loan',
        'loan_request',
        'reservation',
        'app_content',
        'access_log',
        'audit_log',
        'outbox_event'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', seed_table);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', seed_table);
    END LOOP;
END $$;
