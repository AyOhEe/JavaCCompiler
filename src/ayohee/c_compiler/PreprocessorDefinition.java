package ayohee.c_compiler;

import java.util.List;

public abstract class PreprocessorDefinition {
    public abstract void replaceInstances(String label, List<PreprocessingToken> tokens, int i) throws CompilerException;
}
