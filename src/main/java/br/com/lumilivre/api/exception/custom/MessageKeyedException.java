package br.com.lumilivre.api.exception.custom;

public interface MessageKeyedException {

    boolean hasI18nKey();

    String getMessageKey();

    Object[] getMessageArgs();
}
