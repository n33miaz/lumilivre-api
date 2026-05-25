-- =============================================================================
--  R__seed_demo_data.sql
-- -----------------------------------------------------------------------------
--  Dados sinteticos de demonstracao. Este seed e opt-in: inclua
--  classpath:db/seed em LUMILIVRE_FLYWAY_LOCATIONS quando quiser popular
--  ambiente local/dev.
--
--  Cobre todos os status de cada entidade para exercitar a UI completa:
--    - student      (8): 6 sem penalidade, 1 BLOCK, 1 WARNING expirando
--    - book         (30): distribuidos em 10+ generos, com cover_url publica
--    - book_copy    (15): AVAILABLE / BORROWED / MAINTENANCE / UNAVAILABLE
--    - loan         (10): ACTIVE D-10/D-5/D-2/D+1, OVERDUE D+8, 5 COMPLETED
--    - loan_request (6) : PENDING / ACCEPTED / REJECTED / CANCELLED
--    - reservation  (5) : WAITING(x2) / READY / FULFILLED / EXPIRED
--    - thesis       (3) : com pdf_url externo
--    - audit_log    (3) : SUCCESS / FAILURE / DENIED
--    - outbox_event (2) : status SENT
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
        'thesis',
        'audit_log',
        'outbox_event'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I DISABLE ROW LEVEL SECURITY', seed_table);
    END LOOP;
END $$;

-- ----------------------------------------------------------------------------
-- Students (8)
-- ----------------------------------------------------------------------------
INSERT INTO student (
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
--   admin@lumilivre.test     / admin@lumilivre.test
--   librarian@lumilivre.test / 123456
--   2024001..2024008         / matricula igual (senha inicial = matricula)
-- Os hashes foram gerados uma vez para os tres usuarios base; alunos
-- novos ainda nao tem conta (cria-se via fluxo normal). Mantemos so as
-- credenciais demo abaixo para nao explodir esse seed com hashes BCrypt.
-- ----------------------------------------------------------------------------
INSERT INTO app_user (id, email, password_hash, role, student_id, preferred_locale)
VALUES
    ('00000000-0000-4000-8000-000000001001'::uuid, 'admin@lumilivre.test',
        '$2a$10$.gPKMreKYQf0en5npUJxau0lmiKjd9iTfQoTW1mN7z.BPkMAXN2Ay',
        'ADMIN', NULL, 'pt-BR'),
    ('00000000-0000-4000-8000-000000001002'::uuid, 'librarian@lumilivre.test',
        '$2a$10$s0gF584h97IvpZgNzmNz9.ITIq4vhsnT4VZRbz.45XmfHWg3xNJdy',
        'LIBRARIAN', NULL, 'pt-BR'),
    ('00000000-0000-4000-8000-000000001003'::uuid, 'ana.lima@example.com',
        '$2a$10$fHJ73JQxR0RhvAJVYA8ZtuoNyfup0aE1WML5B82x.VSkQigYppugK',
        'STUDENT', '00000000-0000-4000-8000-000000002401'::uuid, 'pt-BR')
ON CONFLICT (email) DO UPDATE SET
    password_hash    = EXCLUDED.password_hash,
    role             = EXCLUDED.role,
    student_id       = EXCLUDED.student_id,
    preferred_locale = EXCLUDED.preferred_locale;

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
INSERT INTO loan (id, borrowed_at, due_at, returned_at, status, student_id, book_copy_id, renewal_count, penalty_code)
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
    student_id    = EXCLUDED.student_id,
    book_copy_id  = EXCLUDED.book_copy_id,
    renewal_count = EXCLUDED.renewal_count,
    penalty_code  = EXCLUDED.penalty_code;

-- ----------------------------------------------------------------------------
-- Loan requests (6)
-- ----------------------------------------------------------------------------
INSERT INTO loan_request (id, student_id, book_copy_id, requested_at, status, note)
VALUES
    ('00000000-0000-4000-8000-000000006001'::uuid, '00000000-0000-4000-8000-000000002403'::uuid, '00000000-0000-4000-8000-000000004004'::uuid, now() - INTERVAL '1 day',  'PENDING',   'Solicitado via seed demo'),
    ('00000000-0000-4000-8000-000000006002'::uuid, '00000000-0000-4000-8000-000000002405'::uuid, '00000000-0000-4000-8000-000000004006'::uuid, now() - INTERVAL '2 days', 'PENDING',   'Aguardando aprovacao'),
    ('00000000-0000-4000-8000-000000006003'::uuid, '00000000-0000-4000-8000-000000002401'::uuid, '00000000-0000-4000-8000-000000004001'::uuid, now() - INTERVAL '12 days','ACCEPTED',  'Aceito e convertido em emprestimo concluido'),
    ('00000000-0000-4000-8000-000000006004'::uuid, '00000000-0000-4000-8000-000000002404'::uuid, '00000000-0000-4000-8000-000000004005'::uuid, now() - INTERVAL '20 days','ACCEPTED',  'Solicitacao aceita historica'),
    ('00000000-0000-4000-8000-000000006005'::uuid, '00000000-0000-4000-8000-000000002406'::uuid, '00000000-0000-4000-8000-000000004007'::uuid, now() - INTERVAL '3 days', 'REJECTED',  'Justificativa indisponivel no horario'),
    ('00000000-0000-4000-8000-000000006006'::uuid, '00000000-0000-4000-8000-000000002402'::uuid, '00000000-0000-4000-8000-000000004008'::uuid, now() - INTERVAL '4 days', 'CANCELLED', 'Cancelado pelo aluno')
ON CONFLICT (id) DO UPDATE SET
    student_id   = EXCLUDED.student_id,
    book_copy_id = EXCLUDED.book_copy_id,
    requested_at = EXCLUDED.requested_at,
    status       = EXCLUDED.status,
    note         = EXCLUDED.note;

-- ----------------------------------------------------------------------------
-- Reservations (5)
-- ----------------------------------------------------------------------------
INSERT INTO reservation (id, student_id, book_id, status, queue_position, expires_at, notified_at)
VALUES
    ('00000000-0000-4000-8000-000000007001'::uuid, '00000000-0000-4000-8000-000000002402'::uuid, '00000000-0000-4000-8000-000000003001'::uuid, 'WAITING',   1, NULL,                          NULL),
    ('00000000-0000-4000-8000-000000007002'::uuid, '00000000-0000-4000-8000-000000002405'::uuid, '00000000-0000-4000-8000-000000003001'::uuid, 'WAITING',   2, NULL,                          NULL),
    ('00000000-0000-4000-8000-000000007003'::uuid, '00000000-0000-4000-8000-000000002406'::uuid, '00000000-0000-4000-8000-000000003020'::uuid, 'READY',     1, now() + INTERVAL '2 days',     now() - INTERVAL '6 hours'),
    ('00000000-0000-4000-8000-000000007004'::uuid, '00000000-0000-4000-8000-000000002401'::uuid, '00000000-0000-4000-8000-000000003019'::uuid, 'FULFILLED', 1, now() - INTERVAL '5 days',     now() - INTERVAL '8 days'),
    ('00000000-0000-4000-8000-000000007005'::uuid, '00000000-0000-4000-8000-000000002404'::uuid, '00000000-0000-4000-8000-000000003017'::uuid, 'EXPIRED',   1, now() - INTERVAL '1 day',      now() - INTERVAL '5 days')
ON CONFLICT (id) DO UPDATE SET
    student_id     = EXCLUDED.student_id,
    book_id        = EXCLUDED.book_id,
    status         = EXCLUDED.status,
    queue_position = EXCLUDED.queue_position,
    expires_at     = EXCLUDED.expires_at,
    notified_at    = EXCLUDED.notified_at;

-- ----------------------------------------------------------------------------
-- Theses (3)
-- ----------------------------------------------------------------------------
INSERT INTO thesis (id, title, authors, advisors, course_id, completion_year, completion_semester, pdf_url, cover_url, external_url, is_active)
SELECT data.id, data.title, data.authors, data.advisors, c.id, data.completion_year, data.completion_semester, data.pdf_url, data.cover_url, data.external_url, TRUE
FROM (
    VALUES
        ('00000000-0000-4000-8000-000000008001'::uuid, 'Sistema de Catalogo Digital para Biblioteca Escolar',     'Ana Beatriz Lima; Mariana Oliveira Santos', 'Prof. Joao Pereira',  'Desenvolvimento de Sistemas', 2025, '2', 'https://www.africau.edu/images/default/sample.pdf',                  'https://covers.openlibrary.org/b/id/10523338-L.jpg', 'https://example.com/lumilivre/tcc-demo-1'),
        ('00000000-0000-4000-8000-000000008002'::uuid, 'Gestao de Estoque para Pequenas Bibliotecas Escolares',   'Carlos Henrique Souza',                     'Profa. Beatriz Nunes','Administração',                2024, '2', 'https://www.africau.edu/images/default/sample.pdf',                  'https://covers.openlibrary.org/b/id/240727-L.jpg',   NULL),
        ('00000000-0000-4000-8000-000000008003'::uuid, 'Automacao de Importacao de Acervos via Planilhas',        'Pedro Henrique Costa; Lucas Pereira Cardoso','Prof. Lucio Almeida','Técnico em Mecatrônica',        2025, '1', 'https://www.africau.edu/images/default/sample.pdf',                  'https://covers.openlibrary.org/b/id/240727-L.jpg',   NULL)
) AS data(id, title, authors, advisors, course_name, completion_year, completion_semester, pdf_url, cover_url, external_url)
JOIN course c ON c.name = data.course_name
ON CONFLICT (id) DO UPDATE SET
    title               = EXCLUDED.title,
    authors             = EXCLUDED.authors,
    advisors            = EXCLUDED.advisors,
    course_id           = EXCLUDED.course_id,
    completion_year     = EXCLUDED.completion_year,
    completion_semester = EXCLUDED.completion_semester,
    pdf_url             = EXCLUDED.pdf_url,
    cover_url           = EXCLUDED.cover_url,
    external_url        = EXCLUDED.external_url,
    is_active           = EXCLUDED.is_active;

-- ----------------------------------------------------------------------------
-- Audit log (3) - tabela append-only sem PK natural; idempotencia via DELETE+INSERT
-- ----------------------------------------------------------------------------
DELETE FROM audit_log WHERE target_id LIKE 'demo-%';

INSERT INTO audit_log (actor, actor_role, target_id, action, result, error_message, occurred_at)
VALUES
    ('admin@lumilivre.test',     'ADMIN',     'demo-loan-001',    'LOAN_CREATED',   'SUCCESS', NULL,                              now() - INTERVAL '6 hours'),
    ('librarian@lumilivre.test', 'LIBRARIAN', 'demo-request-002', 'REQUEST_PROCESSED','FAILURE','Aluno bloqueado por penalidade',  now() - INTERVAL '3 hours'),
    ('ana.lima@example.com',     'STUDENT',   'demo-student-2401','STUDENT_VIEW',   'DENIED',  'Tentativa de acessar outro aluno',now() - INTERVAL '30 minutes');

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
        'student',
        'app_user',
        'book',
        'book_genre',
        'book_copy',
        'loan',
        'loan_request',
        'reservation',
        'thesis',
        'audit_log',
        'outbox_event'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', seed_table);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', seed_table);
    END LOOP;
END $$;
