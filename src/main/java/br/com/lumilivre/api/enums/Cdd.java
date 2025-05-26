package br.com.lumilivre.api.enums;

import java.text.Normalizer;


public enum Cdd implements EnumStatus {
    GENERALIDADES("000"),
    BIBLIOGRAFIA("010"),
    BIBLIOTECA_INFORMATICA("020"),
    ENCICLOPEDIAS("030"),
    NIHIL("040"),
    PUBLICACOES_SERIE("050"),
    ORGANIZACOES_MUSEOGRAFIA("060"),
    MIDIA_JORNALISMO("070"),
    COLECOES("080"),
    MANUSCRITOS_RAROS("090"),
    FILOSOFIA_PSICOLOGIA("100"),
    METAFISICA("110"),
    EPISTEMOLOGIA("120"),
    FENOMENOS_PARANORMAIS("130"),
    ESCOLAS_FILOSOFICAS("140"),
    PSICOLOGIA("150"),
    LOGICA("160"),
    ETICA("170"),
    FILOSOFIA_ANTIGA("180"),
    FILOSOFIA_MODERNA("190"),
    RELIGIAO("200"),
    TEOLOGIA_NATURAL("210"),
    BIBLIA("220"),
    TEOLOGIA_CRISTA("230"),
    MORAL_CRISTA("240"),
    ORDENS_CRISTAS("250"),
    TEOLOGIA_SOCIAL_CRISTA("260"),
    HISTORIA_IGREJA("270"),
    DENOMINACOES_SEITAS("280"),
    OUTRAS_RELIGIOES("290"),
    CIENCIAS_SOCIAIS("300"),
    ESTATISTICA("310"),
    CIENCIA_POLITICA("320"),
    ECONOMIA("330"),
    DIREITO("340"),
    ADMINISTRACAO_PUBLICA("350"),
    PATOLOGIA_SOCIAL("360"),
    EDUCACAO("370"),
    COMERCIO_TRANSPORTE("380"),
    COSTUMES("390"),
    LINGUAS("400"),
    LINGUISTICA("410"),
    INGLES("420"),
    ALEMAO("430"),
    FRANCES("440"),
    ITALIANO("450"),
    ESPANHOL_PORTUGUES("460"),
    LATIM("470"),
    GREGO("480"),
    OUTRAS_LINGUAS("490"),
    CIENCIAS_NATURAIS("500"),
    MATEMATICA("510"),
    ASTRONOMIA("520"),
    FISICA("530"),
    QUIMICA("540"),
    CIENCIAS_TERRA("550"),
    PALEONTOLOGIA("560"),
    CIENCIAS_BIOLOGICAS("570"),
    BOTANICA("580"),
    ZOOLOGIA("590"),
    TECNOLOGIA("600"),
    MEDICINA_SAUDE("610"),
    ENGENHARIA("620"),
    AGRICULTURA("630"),
    ECONOMIA_DOMESTICA("640"),
    ADMINISTRACAO("650"),
    ENGENHARIA_QUIMICA("660"),
    MANUFATURA("670"),
    MANUFATURA_ESPECIFICA("680"),
    CONSTRUCAO("690"),
    ARTES("700"),
    URBANISMO("710"),
    ARQUITETURA("720"),
    ESCULTURA("730"),
    DESENHO_DECORATIVO("740"),
    PINTURA("750"),
    ARTES_GRAFICAS("760"),
    FOTOGRAFIA("770"),
    MUSICA("780"),
    ARTES_RECREATIVAS("790"),
    LITERATURA_RETORICA("800"),
    LITERATURA_AMERICANA("810"),
    LITERATURA_INGLESA("820"),
    LITERATURA_GERMANICA("830"),
    LITERATURA_ROMANCE("840"),
    LITERATURA_ITALIANA("850"),
    LITERATURA_ESPANHOLA("860"),
    LITERATURA_LATINA("870"),
    LITERATURA_GREGA("880"),
    LITERATURA_OUTRAS("890"),
    GEOGRAFIA_HISTORIA("900"),
    GEOGRAFIAS_VIAGENS("910"),
    BIOGRAFIA("920"),
    HISTORIA_ANTIGA("930"),
    HISTORIA_EUROPA("940"),
    HISTORIA_ASIA("950"),
    HISTORIA_AFRICA("960"),
    HISTORIA_AMERICA_NORTE("970"),
    HISTORIA_AMERICA_SUL("980"),
    HISTORIA_OUTRAS_REGIOES("990");

    private final String code;

    Cdd(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return this.name().replace('_', ' ').toLowerCase();
    }

    public static Cdd fromCode(String code) {
        for (Cdd category : values()) {
            if (category.getCode().equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Código não encontrado: " + code);
    }

    public static Cdd fromDescription(String description) {
        String normalized = description.trim().toUpperCase().replace(' ', '_');
        for (Cdd category : values()) {
            if (category.name().equals(normalized)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Descrição não encontrada: " + description);
    }

    public static Cdd searchByPartialDescription(String partialDescription) {
        String normalizedInput = normalizeString(partialDescription);
        for (Cdd category : values()) {
            String normalizedCategory = normalizeString(category.getDescription());
            if (normalizedCategory.contains(normalizedInput)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Nenhuma categoria encontrada contendo: " + partialDescription);
    }

    private static String normalizeString(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase();
    }

	@Override
	public String getStatus() {
		return null;
	}
}
