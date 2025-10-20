package module.domain.app.governance;

public class QuarantinedEventException extends RuntimeException {
    private final String reason;
    public QuarantinedEventException(String reason) {
        super("quarantined: " + reason);
        this.reason = reason;
    }
    public String getReason() { return reason; }
}
