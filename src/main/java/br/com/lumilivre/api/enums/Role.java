package br.com.lumilivre.api.enums;

public enum Role {
    ADMIN("ADMIN"),
    LIBRARIAN("BIBLIOTECARIO"),
    STUDENT("ALUNO");

    private final String ptBrCode;

    Role(String ptBrCode) {
        this.ptBrCode = ptBrCode;
    }

    public String getPtBrCode() {
        return ptBrCode;
    }

    public static Role fromPtBrCode(String code) {
        for (Role r : values()) {
            if (r.ptBrCode.equalsIgnoreCase(code) || r.name().equalsIgnoreCase(code)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown Role code: " + code);
    }
}
