package ayohee.c_compiler;

public class CompilerException extends Exception {
    public CompilerException(PreprocessingContext context) {
        super("Undescribed error occured in " + context.getCurrentFileName() + ":" + context.getLineNumber() + " while compiling " + context.getOriginalSourcePath());
    }
    public CompilerException(PreprocessingContext context, String reason) {
        super("Error occured in " + context.getCurrentFileName() + ":" + context.getLineNumber() + " while compiling " + context.getOriginalSourcePath() + ": " + reason);
    }
    public CompilerException(PreprocessingContext context, String reason, Throwable cause) {
        super("Error occured in " + context.getCurrentFileName() + ":" + context.getLineNumber() + " while compiling " + context.getOriginalSourcePath() + ": " + reason, cause);
    }
}
