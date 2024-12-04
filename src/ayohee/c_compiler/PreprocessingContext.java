package ayohee.c_compiler;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PreprocessingContext {
    private final int MAX_FILE_DEPTH = 32;


    private HashMap<String, PreprocessorDefinition> macros = new HashMap<>();
    private Path sourcePath = null;
    private int fileDepth = 0;
    private boolean yesMode;
    private boolean verbose;

    public PreprocessingContext(Path sourcePath, boolean yesMode, boolean verbose) {
        this.sourcePath = sourcePath;
        this.yesMode = yesMode;
        this.verbose = verbose;
    }

    public boolean isDefined(String identifier) {
        return macros.containsKey(identifier);
    }

    public void define(String identifier, String value) {
        //TODO reject reserved identifiers
        macros.put(identifier, PreprocessorDefinition.parse(value));
    }

    public void undefine(String identifier) {
        macros.remove(identifier);
    }

    public String doReplacement(String line, boolean verbose) {
        for(Map.Entry<String, PreprocessorDefinition> entry : macros.entrySet()) {
            line = entry.getValue().replaceInstances(entry.getKey(), line, verbose);
        }
        line = evaluateConstexprs(line);
        return line;
    }

    public String evaluateConstexprs(String expression) {
        //TODO this
        return expression;
    }

    public void fileDeeper() throws CompilerException {
        fileDepth += 1;
        if (fileDepth > MAX_FILE_DEPTH) {
            throw new CompilerException("Maximum #include depth reached");
        }
    }
    public void fileOut() {
        fileDepth -= 1;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public boolean isVerbose() {
        return verbose;
    }
    public boolean isYesMode() {
        return yesMode;
    }
}
