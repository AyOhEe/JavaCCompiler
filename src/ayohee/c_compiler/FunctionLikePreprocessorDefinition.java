package ayohee.c_compiler;

import java.util.ArrayList;
import java.util.List;

public class FunctionLikePreprocessorDefinition extends PreprocessorDefinition{
    List<String> argumentNames;
    private List<PreprocessingToken> replacementList;

    public FunctionLikePreprocessorDefinition(List<PreprocessingToken> statement) throws CompilerException {
        int replacementListBegin = extractArgumentList(statement);
        extractReplacementList(statement, replacementListBegin);
    }

    private int extractArgumentList(List<PreprocessingToken> statement) throws CompilerException {
        if (!statement.getFirst().is(PreprocessingToken.TokenType.FUNCTIONLIKE_MACRO_DEFINITION)
            || !statement.get(1).is("(")) {
            throw new CompilerException("Invalid function-like macro definition");
        }

        boolean expectingComma = false;
        int i = 2;
        argumentNames = new ArrayList<>();
        for (; i < statement.size(); ++i) {
            PreprocessingToken currentToken = statement.get(i);
            if (currentToken.is(")")) {
                break;
            }


            if (expectingComma && currentToken.is(",")) {
                expectingComma = false;
                continue;
            } else if (!expectingComma && currentToken.is(",")) {
                throw new CompilerException("Improperly formed function-like macro definition");
            }

            if (!expectingComma && currentToken.is(PreprocessingToken.TokenType.IDENTIFIER)) {
                argumentNames.addLast(currentToken.toString());
                expectingComma = true;
            } else if (expectingComma && currentToken.is(PreprocessingToken.TokenType.IDENTIFIER)) {
                throw new CompilerException("Improperly formed function-like macro definition");
            }
        }

        return i + 1;
    }

    private void extractReplacementList(List<PreprocessingToken> statement, int replacementListBegin) {
        replacementList = new ArrayList<>();
        if (statement.size() <= replacementListBegin) {
            return;
        }

        replacementList.addAll(statement.subList(replacementListBegin, statement.size()));
    }

    @Override
    public void replaceInstances(String label, List<PreprocessingToken> tokens, int i) throws CompilerException {
        //TODO
    }
}
