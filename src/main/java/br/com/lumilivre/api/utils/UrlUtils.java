package br.com.lumilivre.api.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UrlUtils {

    /**
     * Gera uma URL válida a partir do nome do arquivo e do caminho base
     */
    public static String gerarUrlValida(String baseUrl, String caminho, String nomeArquivo) {
        if (nomeArquivo == null) return null;

        // Escapa caracteres especiais do arquivo
        String arquivoEncode = URLEncoder.encode(nomeArquivo, StandardCharsets.UTF_8);

        // Ajusta barras
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        if (caminho.startsWith("/")) caminho = caminho.substring(1);
        if (!caminho.endsWith("/")) caminho += "/";

        return baseUrl + caminho + arquivoEncode;
    }
}
