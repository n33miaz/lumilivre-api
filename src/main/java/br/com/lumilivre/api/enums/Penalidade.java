package br.com.lumilivre.api.enums;

public enum Penalidade {

    REGISTRO("Registro", 1),
    ADVERTENCIA("Advertência", 2),
    SUSPENSAO("Suspensão", 3),
    BLOQUEIO("Bloqueio", 4),
    BANIMENTO("Banimento", 5);

    private final String status;
    private final int gravidade;

    Penalidade(String status, int gravidade) {
        this.status = status;
        this.gravidade = gravidade;
    }

    public String getStatus() {
        return status;
    }

    public int getGravidade() {
        return gravidade;
    }

    public static Penalidade fromDiasDeAtraso(long diasDeAtraso) {
        if (diasDeAtraso <= 1) {
            return REGISTRO;
        }
        if (diasDeAtraso <= 5) {
            return ADVERTENCIA;
        }
        if (diasDeAtraso <= 7) {
            return SUSPENSAO;
        }
        if (diasDeAtraso <= 90) {
            return BLOQUEIO;
        }
        return BANIMENTO;
    }

    public boolean isMaisGraveQue(Penalidade outra) {

        if (outra == null) {
            return true;
        }
        return this.gravidade > outra.getGravidade();
    }
}