package br.com.fiap.fordchallengebackend.exception;

public class ExternalIntegrationException extends RuntimeException {

    public ExternalIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
