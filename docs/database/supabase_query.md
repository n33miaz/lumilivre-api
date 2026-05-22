-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.academic_module (
  id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
  name character varying NOT NULL UNIQUE,
  CONSTRAINT academic_module_pkey PRIMARY KEY (id)
);
CREATE TABLE public.app_user (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  email USER-DEFINED NOT NULL UNIQUE,
  password_hash character varying,
  role character varying NOT NULL CHECK (role::text = ANY (ARRAY['ADMIN'::character varying, 'LIBRARIAN'::character varying, 'STUDENT'::character varying]::text[])),
  student_id uuid UNIQUE,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  deleted_at timestamp with time zone,
  preferred_locale character varying NOT NULL DEFAULT 'pt-BR'::character varying,
  CONSTRAINT app_user_pkey PRIMARY KEY (id),
  CONSTRAINT fk_app_user_student FOREIGN KEY (student_id) REFERENCES public.student(id)
);
CREATE TABLE public.audit_log (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  actor character varying NOT NULL,
  actor_role character varying NOT NULL,
  target_id character varying,
  action character varying NOT NULL,
  result character varying NOT NULL CHECK (result::text = ANY (ARRAY['SUCCESS'::character varying, 'FAILURE'::character varying, 'DENIED'::character varying]::text[])),
  error_message text,
  occurred_at timestamp with time zone NOT NULL,
  CONSTRAINT audit_log_pkey PRIMARY KEY (id)
);
CREATE TABLE public.book (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  isbn character varying,
  title character varying NOT NULL,
  publication_date date CHECK (publication_date IS NULL OR publication_date <= CURRENT_DATE),
  page_count integer CHECK (page_count IS NULL OR page_count > 0),
  dewey_code character varying,
  publisher character varying NOT NULL,
  age_rating character varying NOT NULL,
  edition character varying,
  volume integer,
  synopsis text,
  author character varying,
  cover_type character varying,
  cover_url character varying,
  rating double precision DEFAULT 4.6 CHECK (rating IS NULL OR rating >= 0::double precision AND rating <= 5::double precision),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  deleted_at timestamp with time zone,
  CONSTRAINT book_pkey PRIMARY KEY (id),
  CONSTRAINT fk_book_dewey FOREIGN KEY (dewey_code) REFERENCES public.dewey_classification(code)
);
CREATE TABLE public.book_copy (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  copy_code character varying NOT NULL UNIQUE,
  status character varying NOT NULL DEFAULT 'AVAILABLE'::character varying CHECK (status::text = ANY (ARRAY['AVAILABLE'::character varying, 'BORROWED'::character varying, 'UNAVAILABLE'::character varying, 'MAINTENANCE'::character varying]::text[])),
  book_id uuid NOT NULL,
  shelf_location character varying NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  deleted_at timestamp with time zone,
  CONSTRAINT book_copy_pkey PRIMARY KEY (id),
  CONSTRAINT fk_book_copy_book FOREIGN KEY (book_id) REFERENCES public.book(id)
);
CREATE TABLE public.book_genre (
  book_id uuid NOT NULL,
  genre_id integer NOT NULL,
  CONSTRAINT book_genre_pkey PRIMARY KEY (book_id, genre_id),
  CONSTRAINT fk_book_genre_book FOREIGN KEY (book_id) REFERENCES public.book(id),
  CONSTRAINT fk_book_genre_genre FOREIGN KEY (genre_id) REFERENCES public.genre(id)
);
CREATE TABLE public.course (
  id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
  name character varying NOT NULL UNIQUE,
  CONSTRAINT course_pkey PRIMARY KEY (id)
);
CREATE TABLE public.dewey_classification (
  code character varying NOT NULL,
  description character varying,
  CONSTRAINT dewey_classification_pkey PRIMARY KEY (code)
);
CREATE TABLE public.flyway_schema_history (
  installed_rank integer NOT NULL,
  version character varying,
  description character varying NOT NULL,
  type character varying NOT NULL,
  script character varying NOT NULL,
  checksum integer,
  installed_by character varying NOT NULL,
  installed_on timestamp without time zone NOT NULL DEFAULT now(),
  execution_time integer NOT NULL,
  success boolean NOT NULL,
  CONSTRAINT flyway_schema_history_pkey PRIMARY KEY (installed_rank)
);
CREATE TABLE public.genre (
  id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
  name character varying NOT NULL UNIQUE,
  CONSTRAINT genre_pkey PRIMARY KEY (id)
);
CREATE TABLE public.loan (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  borrowed_at timestamp with time zone NOT NULL,
  due_at timestamp with time zone NOT NULL,
  returned_at timestamp with time zone,
  penalty_code character varying,
  status character varying NOT NULL DEFAULT 'ACTIVE'::character varying CHECK (status::text = ANY (ARRAY['ACTIVE'::character varying, 'COMPLETED'::character varying, 'OVERDUE'::character varying]::text[])),
  student_id uuid NOT NULL,
  book_copy_id uuid NOT NULL,
  renewal_count integer NOT NULL DEFAULT 0 CHECK (renewal_count >= 0),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT loan_pkey PRIMARY KEY (id),
  CONSTRAINT fk_loan_student FOREIGN KEY (student_id) REFERENCES public.student(id),
  CONSTRAINT fk_loan_book_copy FOREIGN KEY (book_copy_id) REFERENCES public.book_copy(id)
);
CREATE TABLE public.loan_request (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  student_id uuid NOT NULL,
  book_copy_id uuid NOT NULL,
  requested_at timestamp with time zone NOT NULL DEFAULT now(),
  status character varying NOT NULL DEFAULT 'PENDING'::character varying CHECK (status::text = ANY (ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'REJECTED'::character varying, 'CANCELLED'::character varying]::text[])),
  note character varying,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT loan_request_pkey PRIMARY KEY (id),
  CONSTRAINT fk_loan_request_student FOREIGN KEY (student_id) REFERENCES public.student(id),
  CONSTRAINT fk_loan_request_book_copy FOREIGN KEY (book_copy_id) REFERENCES public.book_copy(id)
);
CREATE TABLE public.outbox_event (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  event_type character varying NOT NULL,
  recipient_email character varying NOT NULL,
  subject character varying NOT NULL,
  body text NOT NULL,
  status character varying NOT NULL DEFAULT 'PENDING'::character varying CHECK (status::text = ANY (ARRAY['PENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying, 'DEAD_LETTER'::character varying]::text[])),
  retry_count integer NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  processed_at timestamp with time zone,
  next_retry_at timestamp with time zone,
  CONSTRAINT outbox_event_pkey PRIMARY KEY (id)
);
CREATE TABLE public.password_reset_token (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  token character varying NOT NULL UNIQUE,
  app_user_id uuid NOT NULL UNIQUE,
  expires_at timestamp with time zone NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT password_reset_token_pkey PRIMARY KEY (id),
  CONSTRAINT fk_password_reset_token_app_user FOREIGN KEY (app_user_id) REFERENCES public.app_user(id)
);
CREATE TABLE public.reservation (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  student_id uuid NOT NULL,
  book_id uuid NOT NULL,
  status character varying NOT NULL DEFAULT 'WAITING'::character varying CHECK (status::text = ANY (ARRAY['WAITING'::character varying, 'READY'::character varying, 'CANCELLED'::character varying, 'EXPIRED'::character varying, 'FULFILLED'::character varying]::text[])),
  queue_position integer NOT NULL CHECK (queue_position > 0),
  expires_at timestamp with time zone,
  notified_at timestamp with time zone,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT reservation_pkey PRIMARY KEY (id),
  CONSTRAINT fk_reservation_student FOREIGN KEY (student_id) REFERENCES public.student(id),
  CONSTRAINT fk_reservation_book FOREIGN KEY (book_id) REFERENCES public.book(id)
);
CREATE TABLE public.student (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  registration_number character varying NOT NULL UNIQUE,
  full_name character varying NOT NULL,
  avatar_url character varying,
  cpf character varying CHECK (cpf IS NULL OR cpf::text ~ '^[0-9]{11}$'::text),
  birth_date date,
  phone_number character varying,
  email USER-DEFINED,
  course_id integer NOT NULL,
  academic_module_id integer NOT NULL,
  study_shift_id integer NOT NULL,
  postal_code character varying,
  street character varying,
  address_complement character varying,
  district character varying,
  city character varying,
  state_code character CHECK (state_code IS NULL OR state_code ~ '^[A-Z]{2}$'::text),
  street_number integer,
  penalty_code character varying,
  penalty_expires_at timestamp with time zone,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  deleted_at timestamp with time zone,
  CONSTRAINT student_pkey PRIMARY KEY (id),
  CONSTRAINT fk_student_course FOREIGN KEY (course_id) REFERENCES public.course(id),
  CONSTRAINT fk_student_academic_module FOREIGN KEY (academic_module_id) REFERENCES public.academic_module(id),
  CONSTRAINT fk_student_study_shift FOREIGN KEY (study_shift_id) REFERENCES public.study_shift(id)
);
CREATE TABLE public.study_shift (
  id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
  name character varying NOT NULL UNIQUE,
  CONSTRAINT study_shift_pkey PRIMARY KEY (id)
);
CREATE TABLE public.thesis (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  title character varying NOT NULL,
  authors character varying NOT NULL,
  advisors character varying,
  course_id integer NOT NULL,
  completion_year integer CHECK (completion_year IS NULL OR completion_year >= 1900 AND completion_year <= 2100),
  completion_semester character varying,
  pdf_url character varying,
  cover_url character varying,
  external_url character varying,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  deleted_at timestamp with time zone,
  CONSTRAINT thesis_pkey PRIMARY KEY (id),
  CONSTRAINT fk_thesis_course FOREIGN KEY (course_id) REFERENCES public.course(id)
);