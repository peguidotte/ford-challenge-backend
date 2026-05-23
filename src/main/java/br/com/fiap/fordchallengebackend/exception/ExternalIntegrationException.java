package br.com.fiap.fordchallengebackend.exception;

public class ExternalIntegrationException extends RuntimeException {

    private final Integer upstreamStatus;
    private final String upstreamBody;

    public ExternalIntegrationException(String message, Throwable cause) {
        this(message, cause, null, null);
    }

    public ExternalIntegrationException(String message, Throwable cause, Integer upstreamStatus, String upstreamBody) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
        this.upstreamBody = upstreamBody;
    }

    public Integer upstreamStatus() {
        return upstreamStatus;
    }

    public String upstreamBody() {
        return upstreamBody;
    }
}
