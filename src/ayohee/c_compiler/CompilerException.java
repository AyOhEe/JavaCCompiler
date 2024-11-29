package ayohee.c_compiler;

public class CompilerException extends Exception {
    public CompilerException() {
        super();
    }
    public CompilerException(String reason) {
        super(reason);
    }
    public CompilerException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
