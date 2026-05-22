package br.com.lumilivre.api.config;

import java.util.List;

public final class SwaggerTags {

    public static final String AUTH = "auth";
    public static final String USERS = "users";
    public static final String STUDENTS = "students";
    public static final String BOOKS = "books";
    public static final String BOOK_COPIES = "book-copies";
    public static final String LOANS = "loans";
    public static final String LOAN_REQUESTS = "loan-requests";
    public static final String RESERVATIONS = "reservations";
    public static final String THESES = "theses";
    public static final String DASHBOARD = "dashboard";
    public static final String REPORTS = "reports";
    public static final String IMPORTS = "imports";
    public static final String METADATA = "metadata";
    public static final String COURSES = "courses";
    public static final String STUDY_SHIFTS = "study-shifts";
    public static final String ACADEMIC_MODULES = "academic-modules";
    public static final String GENRES = "genres";
    public static final String DEWEY_CLASSIFICATIONS = "dewey-classifications";
    public static final String SYSTEM = "system";

    public static final List<String> ORDERED = List.of(
            AUTH,
            USERS,
            STUDENTS,
            BOOKS,
            BOOK_COPIES,
            LOANS,
            LOAN_REQUESTS,
            RESERVATIONS,
            THESES,
            DASHBOARD,
            REPORTS,
            IMPORTS,
            METADATA,
            COURSES,
            STUDY_SHIFTS,
            ACADEMIC_MODULES,
            GENRES,
            DEWEY_CLASSIFICATIONS,
            SYSTEM
    );

    private SwaggerTags() {
    }
}
