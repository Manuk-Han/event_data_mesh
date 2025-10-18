package module.domain.app.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("email exists: " + email);
    }
}
