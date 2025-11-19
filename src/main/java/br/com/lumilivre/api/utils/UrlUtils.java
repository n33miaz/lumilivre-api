package br.com.lumilivre.api.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UrlUtils {

    public static String gerarUrlValida(String baseUrl, String caminho, String nomeArquivo) {
        if (nomeArquivo == null)
            return null;

        String arquivoEncode = URLEncoder.encode(nomeArquivo, StandardCharsets.UTF_8);

        if (!baseUrl.endsWith("/"))
            baseUrl += "/";
        if (caminho.startsWith("/"))
            caminho = caminho.substring(1);
        if (!caminho.endsWith("/"))
            caminho += "/";

        return baseUrl + caminho + arquivoEncode;
    }
}
