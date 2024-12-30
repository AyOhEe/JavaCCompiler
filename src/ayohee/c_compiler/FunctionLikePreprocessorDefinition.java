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
        if (!tokens.get(i).is(label) || (tokens.size() > i + 1 && !tokens.get(i + 1).is("("))) {
            return;
        }
        tokens.remove(i); // identifier
        tokens.remove(i); // '('

        List<List<PreprocessingToken>> argumentTokens = new ArrayList<>();
        int tokensToRemove = extractArgumentsFromInvocation(argumentTokens, tokens, i);
        for (int j = 0; j < tokensToRemove; ++j) {
            tokens.remove(i);
        }

        List<PreprocessingToken> replacement = generateReplacement(argumentTokens);
        tokens.addAll(i, replacement);

        System.out.println("Function-like invocation: " + label);
    }

    private int extractArgumentsFromInvocation(List<List<PreprocessingToken>> argumentTokens, List<PreprocessingToken> tokens, int i) {
        int tokensToRemove = 0;
        int parenDepth = 0;
        PreprocessingToken currentToken = tokens.get(i);
        List<PreprocessingToken> currentArgument = new ArrayList<>();
        while (!(parenDepth == 0 && currentToken.is(")"))) {
            if (currentToken.is("(")) {
                ++parenDepth;
                currentArgument.add(currentToken);
            } else if (currentToken.is(")")) {
                --parenDepth;
                currentArgument.add(currentToken);
            } else if (parenDepth == 0 && currentToken.is(",")) {
                argumentTokens.add(currentArgument);
                currentArgument = new ArrayList<>();
            } else {
                currentArgument.add(currentToken);
            }

            ++i;
            tokensToRemove++;
            currentToken = tokens.get(i);
        }

        argumentTokens.add(currentArgument);
        ++tokensToRemove; //get rid of the ")" too
        return tokensToRemove;
    }

    private List<PreprocessingToken> generateReplacement(List<List<PreprocessingToken>> argumentTokens) {
        List<PreprocessingToken> replacement = new ArrayList<>(replacementList);

        for (int i = 0; i < replacement.size(); ++i) {
            for (int j = 0; j < argumentNames.size(); ++j) {
                if (replacement.get(i).is(argumentNames.get(j))) {
                    replacement.remove(i);
                    replacement.addAll(i, argumentTokens.get(j));
                    --i; //stay on this token index: the -- and ++ cancel out.
                }
            }
        }

        return replacement;
    }
}
