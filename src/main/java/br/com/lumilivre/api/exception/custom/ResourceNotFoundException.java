package br.com.lumilivre.api.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public ResourceNotFoundException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = null;
    }

    private ResourceNotFoundException(String key, Object[] args) {
        super(key);
        this.messageKey = key;
        this.messageArgs = args;
    }

    public static ResourceNotFoundException ofKey(String key, Object... args) {
        return new ResourceNotFoundException(key, args);
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
