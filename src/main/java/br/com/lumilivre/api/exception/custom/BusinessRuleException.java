package br.com.lumilivre.api.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessRuleException extends RuntimeException implements MessageKeyedException {

    private final String messageKey;
    private final Object[] messageArgs;

    public BusinessRuleException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = null;
    }

    private BusinessRuleException(String key, Object[] args) {
        super(key);
        this.messageKey = key;
        this.messageArgs = args;
    }

    public static BusinessRuleException ofKey(String key, Object... args) {
        return new BusinessRuleException(key, args);
    }

    public boolean hasI18nKey() {
        return messageKey != null;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs;
    }
}
