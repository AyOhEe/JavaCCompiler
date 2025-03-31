package ayohee.c_compiler;

import java.util.ArrayList;
import java.util.List;

public class FunctionLikePreprocessorDefinition extends PreprocessorDefinition{
    List<String> argumentNames;
    private List<PreprocessingToken> replacementList;

    public FunctionLikePreprocessorDefinition(List<PreprocessingToken> statement, PreprocessingContext context) throws CompilerException {
        int replacementListBegin = extractArgumentList(statement, context);
        extractReplacementList(statement, replacementListBegin);
    }

    private int extractArgumentList(List<PreprocessingToken> statement, PreprocessingContext context) throws CompilerException {
        if (!statement.getFirst().is(PreprocessingToken.TokenType.FUNCTIONLIKE_MACRO_DEFINITION)
            || !statement.get(1).is("(")) {
            throw new CompilerException(context, "Invalid function-like macro definition");
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
                throw new CompilerException(context, "Improperly formed function-like macro definition");
            }

            if (!expectingComma && currentToken.is(PreprocessingToken.TokenType.IDENTIFIER)) {
                argumentNames.addLast(currentToken.toString());
                expectingComma = true;
            } else if (expectingComma && currentToken.is(PreprocessingToken.TokenType.IDENTIFIER)) {
                throw new CompilerException(context, "Improperly formed function-like macro definition");
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
    public boolean  replaceInstances(String label, List<PreprocessingToken> tokens, int i) throws CompilerException {
        if (!tokens.get(i).is(label) || (tokens.size() > i + 1 && !tokens.get(i + 1).is("("))) {
            return false;
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

        return true;
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
            if (replacement.get(i).is("#")) {
                //subtract one to counteract the ++i at the end of the loop
                i = processStringifyOperator(replacement, i, argumentTokens) - 1;
                continue;
            }
            if (replacement.get(i).is("##")) {
                //subtract one to counteract the ++i at the end of the loop
                i = processTokenPasteOperator(replacement, i, argumentTokens) - 1;
                continue;
            }

            for (int j = 0; j < argumentNames.size(); ++j) {
                if (replacement.get(i).is(argumentNames.get(j))) {
                    replacement.remove(i);
                    replacement.addAll(i, argumentTokens.get(j));
                    i += argumentTokens.get(j).size(); //jump ahead to skip the tokens we replaced
                    --i; //but still counteract the ++i at the end of the loop
                }
            }
        }

        return replacement;
    }

    private int processStringifyOperator(List<PreprocessingToken> replacement, int i, List<List<PreprocessingToken>> argumentTokens) {
        //ensure next token exists
        int targetIndex = i + 1;
        if (targetIndex > replacement.size()) {
            throw new IndexOutOfBoundsException("Stringify operator (#) at end of function-like invocation");
        }

        //find the corresponding argument
        PreprocessingToken targetToken = replacement.get(targetIndex);
        int argumentIndex = -1;
        for (int j = 0; j < argumentNames.size(); ++j) {
            if (targetToken.is(argumentNames.get(j))) {
                argumentIndex = j;
                break;
            }
        }
        if (argumentIndex == -1) {
            throw new IndexOutOfBoundsException("Stringify (#) argument was not a macro parameter");
        }

        //extract the argument and stringify it
        List<PreprocessingToken> argument = argumentTokens.get(argumentIndex);
        StringBuilder stringifyResult = new StringBuilder();

        if (!argument.isEmpty()) {
            stringifyResult.append(argument.getFirst());
        }
        for (int j = 1; j < argument.size(); ++j) {
            stringifyResult.append(" ").append(argument.get(j).toString());
        }

        //append the result as a token
        replacement.remove(i); // #
        replacement.remove(i); // identifier
        replacement.add(i, new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, stringifyResult.toString()));

        //start at the next token
        return i + 1;
    }

    private int processTokenPasteOperator(List<PreprocessingToken> replacement, int i, List<List<PreprocessingToken>> argumentTokens) {
        return i + 1;
    }
}
