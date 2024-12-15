package ayohee.c_compiler;

import java.util.List;

public abstract class PreprocessorDefinition {
    public abstract void replaceInstances(String label, List<String> lines, int i, boolean verbose) throws CompilerException;
}
