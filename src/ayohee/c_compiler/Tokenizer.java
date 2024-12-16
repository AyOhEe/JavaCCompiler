package ayohee.c_compiler;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    public static List<PreprocessingToken> tokenize(String workingContents, boolean verbose) {
        //TODO this
        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.OTHER, workingContents));
        return tokens;
    }
}
