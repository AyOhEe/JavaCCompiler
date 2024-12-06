package ayohee.c_compiler;

public class FunctionLikePreprocessorDefinition extends PreprocessorDefinition {
    public FunctionLikePreprocessorDefinition(String line, int lParenIndex) {

    }

    @Override
    public String replaceInstances(String label, String line, boolean verbose) {
        return line;
    }
}
