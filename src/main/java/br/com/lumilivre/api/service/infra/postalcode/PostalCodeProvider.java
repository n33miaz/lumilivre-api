package br.com.lumilivre.api.service.infra.postalcode;

import java.util.Optional;
import java.util.Set;

/**
 * Strategy de consulta de código postal. Cada provider declara os países que
 * suporta e implementa o lookup com sua API externa. O roteador escolhe o
 * primeiro provider que declara suportar o país consultado.
 */
public interface PostalCodeProvider {

    String name();

    /**
     * Códigos ISO 3166-1 alpha-2 cobertos. Use {@code "*"} para um provider
     * universal (cobre os demais países como fallback).
     */
    Set<String> supportedCountryCodes();

    /**
     * Faz lookup e retorna o endereço normalizado. {@link Optional#empty()}
     * quando o code não existe — providers nunca devem lançar para 404.
     */
    Optional<PostalAddress> lookup(String code, String countryCode);
}
