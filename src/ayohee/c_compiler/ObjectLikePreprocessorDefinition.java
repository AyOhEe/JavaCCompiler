package ayohee.c_compiler;

import java.util.List;
import java.util.stream.Collectors;

public class ObjectLikePreprocessorDefinition extends PreprocessorDefinition{
    private List<PreprocessingToken> replacementList;

    public ObjectLikePreprocessorDefinition(List<PreprocessingToken> replacementList) {
        this.replacementList = replacementList;
    }

    @Override
    public boolean replaceInstances(String label, List<PreprocessingToken> tokens, int i) throws CompilerException {
        if (tokens.get(i).is(label)) {
            tokens.remove(i);
            tokens.addAll(i, replacementList);

            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return replacementList.stream().map(PreprocessingToken::toString).collect(Collectors.joining(" "));
    }
}
