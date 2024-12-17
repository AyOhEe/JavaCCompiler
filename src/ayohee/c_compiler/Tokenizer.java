package ayohee.c_compiler;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    public static List<PreprocessingToken> tokenize(String workingContents, PreprocessingContext context) throws CompilerException {
        //TODO this
        List<PreprocessingToken> tokens = new ArrayList<>();
        //tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.OTHER, workingContents));


        for (int i = 0; i < workingContents.length();) {
            i = parseNextToken(tokens, workingContents, i, context);
        }

        return tokens;
    }

    private static int parseNextToken(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) throws CompilerException {
        for (int j = i; j < workingContents.length() && Character.isWhitespace(workingContents.charAt(j)); ++j) {
            if (workingContents.charAt(j) == '\n') {
                tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));
            }
        }

        int nextChar = -1;

        //header names must be searched first - they may otherwise register as string literals and identifiers
        boolean isIncludeStatement = isIncludeStatement(tokens);
        if (isIncludeStatement && (nextChar = tryGetHeaderName(tokens, workingContents, i, context)) != -1) {
            return nextChar;
        }
        if ((nextChar = tryGetIdentifier(tokens, workingContents, i, context)) != -1) {
            return nextChar;
        }
        if ((nextChar = tryGetPPNumber(tokens, workingContents, i, context)) != -1) {
            return nextChar;
        }
        if ((nextChar = tryGetCharConst(tokens, workingContents, i, context)) != -1) {
            return nextChar;
        }
        if ((nextChar = tryGetStringLiteral(tokens, workingContents, i, context)) != -1) {
            return nextChar;
        }
        if ((nextChar = tryGetOperatorPunctuator(tokens, workingContents, i, context)) != -1) {
            return nextChar;
        }

        throw new CompilerException(
                "Unsure of token: "
                + context.getCurrentSourcePath()
                + ":"
                + i
                + " "
                + workingContents.substring(i, Math.max(workingContents.length(), i + 20))
        );
    }

    private static boolean isIncludeStatement(List<PreprocessingToken> tokens) {
        PreprocessingToken lastToken = tokens.isEmpty() ? null : tokens.getLast();
        PreprocessingToken secondLastToken = tokens.size() >= 2 ? tokens.get(tokens.size() - 2) : null;
        return lastToken != null
                && secondLastToken != null
                && lastToken.getType() == PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR
                && secondLastToken.getType() == PreprocessingToken.TokenType.IDENTIFIER
                && lastToken.toString().contentEquals("#");
    }

    private static int tryGetHeaderName(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) throws CompilerException {
        if (workingContents.charAt(i) != '<' && workingContents.charAt(i) != '"') {
            return -1;
        }
        char stopChar = workingContents.charAt(i) == '<' ? '>' : '"';

        int startName = i;
        while (i < workingContents.length() && workingContents.charAt(i) != stopChar) {
            if (workingContents.charAt(i) == '\n') {
                throw new CompilerException("Incomplete header name: " + context.getCurrentSourcePath() + ":" + i);
            }

            ++i;
        }

        String headerName = workingContents.substring(startName, i);
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.HEADER_NAME, headerName));

        return i;
    }

    private static int tryGetIdentifier(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) {

    }

    private static int tryGetPPNumber(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) {

    }

    private static int tryGetCharConst(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) {

    }

    private static int tryGetStringLiteral(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) {

    }

    private static int tryGetOperatorPunctuator(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) {

    }


    private static String escapeString(String unescaped) {
        return unescaped
                .replace("\\\\", "\\")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\'", "'");
    }
}
