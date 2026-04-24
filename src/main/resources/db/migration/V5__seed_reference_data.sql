-- =============================================================================
--  V5__seed_reference_data.sql
-- -----------------------------------------------------------------------------
--  Dados de referencia do dominio (PT-BR por natureza do negocio).
--  Idempotente via ON CONFLICT. Nao contem dados de negocio (emprestimos,
--  alunos, livros) - esses ficam em V6__seed_demo_data (PR 6).
-- =============================================================================

-- ----------------------------------------------------------------------------
-- course
-- ----------------------------------------------------------------------------
INSERT INTO course (name) VALUES
    ('Administracao'),
    ('Desenvolvimento de Sistemas'),
    ('Logistica'),
    ('Recursos Humanos'),
    ('Seguranca do Trabalho'),
    ('Tecnico em Enfermagem'),
    ('Tecnico em Contabilidade'),
    ('Tecnico em Mecatronica')
ON CONFLICT (name) DO NOTHING;

-- ----------------------------------------------------------------------------
-- academic_module
-- ----------------------------------------------------------------------------
INSERT INTO academic_module (name) VALUES
    ('Modulo 1'),
    ('Modulo 2'),
    ('Modulo 3'),
    ('Modulo 4'),
    ('Modulo Basico'),
    ('Egresso')
ON CONFLICT (name) DO NOTHING;

-- ----------------------------------------------------------------------------
-- study_shift
-- ----------------------------------------------------------------------------
INSERT INTO study_shift (name) VALUES
    ('Matutino'),
    ('Vespertino'),
    ('Noturno'),
    ('Integral')
ON CONFLICT (name) DO NOTHING;

-- ----------------------------------------------------------------------------
-- genre
-- ----------------------------------------------------------------------------
INSERT INTO genre (name) VALUES
    ('Romance'),
    ('Ficcao Cientifica'),
    ('Fantasia'),
    ('Historia'),
    ('Biografia'),
    ('Tecnologia'),
    ('Administracao'),
    ('Saude'),
    ('Educacao'),
    ('Didatico'),
    ('Infantojuvenil'),
    ('Suspense'),
    ('Poesia'),
    ('Quadrinhos'),
    ('Autoajuda'),
    ('Classicos')
ON CONFLICT (name) DO NOTHING;

-- ----------------------------------------------------------------------------
-- dewey_classification (resumo das classes principais da CDD)
--   Detalhamento completo pode ser carregado depois via importacao.
-- ----------------------------------------------------------------------------
INSERT INTO dewey_classification (code, description) VALUES
    ('000', 'Generalidades, Ciencia da Computacao e Informacao'),
    ('100', 'Filosofia e Psicologia'),
    ('200', 'Religiao'),
    ('300', 'Ciencias Sociais'),
    ('400', 'Linguagem'),
    ('500', 'Ciencias Naturais e Matematica'),
    ('600', 'Tecnologia (Ciencias Aplicadas)'),
    ('700', 'Artes e Recreacao'),
    ('800', 'Literatura'),
    ('900', 'Geografia, Historia e Biografia'),
    ('004', 'Processamento de dados e Ciencia da Computacao'),
    ('005', 'Programacao, programas e dados de computador'),
    ('006', 'Metodos especiais de computacao'),
    ('150', 'Psicologia'),
    ('370', 'Educacao'),
    ('510', 'Matematica'),
    ('610', 'Medicina e Saude'),
    ('630', 'Agricultura e tecnologias relacionadas'),
    ('650', 'Administracao e servicos auxiliares'),
    ('658', 'Administracao geral'),
    ('660', 'Engenharia quimica e tecnologias relacionadas'),
    ('869', 'Literaturas portuguesa e brasileira')
ON CONFLICT (code) DO NOTHING;
