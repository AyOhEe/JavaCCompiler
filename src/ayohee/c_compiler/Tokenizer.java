package ayohee.c_compiler;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    public static List<PreprocessingToken> tokenize(String workingContents, PreprocessingContext context) throws CompilerException {
        List<PreprocessingToken> tokens = new ArrayList<>();
        for (int i = 0; i < workingContents.length();) {
            try {
                i = parseNextToken(tokens, workingContents, i, context);
            } catch (StringIndexOutOfBoundsException e) {
                throw new CompilerException("File " + context.getCurrentSourcePath() + " ended in incomplete preprocessing token", e);
            }
        }

        return tokens;
    }

    private static int parseNextToken(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) throws CompilerException {
        int j = i;
        for (; j < workingContents.length(); ++j) {
            if (!Character.isWhitespace(workingContents.charAt(j))) {
                break;
            }

            if (workingContents.charAt(j) == '\n') {
                tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));
                return j + 1;
            }
        }

        int nextChar = -1;

        //header names must be searched first - they may otherwise register as string literals and identifiers
        boolean isIncludeStatement = isIncludeStatement(tokens);
        if (isIncludeStatement && (nextChar = tryGetHeaderName(tokens, workingContents, j, context)) != -1) {
            return nextChar;
        }
        if ((nextChar = tryGetPPNumber(tokens, workingContents, j, context)) != -1) {
            return nextChar;
        }
        if ((nextChar = tryGetCharConst(tokens, workingContents, j, context)) != -1) {
            return nextChar;
        }
        if ((nextChar = tryGetStringLiteral(tokens, workingContents, j, context)) != -1) {
            return nextChar;
        }
        if ((nextChar = tryGetOperatorPunctuator(tokens, workingContents, j, context)) != -1) {
            return nextChar;
        }
        if ((nextChar = tryGetIdentifier(tokens, workingContents, j, context)) != -1) {
            return nextChar;
        }

        throw new CompilerException(
                "Unsure of token: "
                + context.getCurrentSourcePath()
                + ":"
                + j
                + " "
                + workingContents.substring(i, Math.min(workingContents.length(), i + 20))
        );
    }

    private static boolean isIncludeStatement(List<PreprocessingToken> tokens) {
        PreprocessingToken lastToken = tokens.isEmpty() ? null : tokens.getLast();
        PreprocessingToken secondLastToken = tokens.size() >= 2 ? tokens.get(tokens.size() - 2) : null;
        return lastToken != null
                && secondLastToken != null
                && lastToken.getType() == PreprocessingToken.TokenType.IDENTIFIER
                && lastToken.toString().contentEquals("include")
                && secondLastToken.getType() == PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR
                && secondLastToken.toString().contentEquals("#");
    }

    private static int tryGetHeaderName(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) throws CompilerException {
        if (workingContents.charAt(i) != '<' && workingContents.charAt(i) != '"') {
            return -1;
        }
        char startChar = workingContents.charAt(i) == '<' ? '<' : '"';
        char stopChar = workingContents.charAt(i) == '<' ? '>' : '"';

        i += 1; //skip the starting character so the first quote doesn't immediately stop the parsing
        int startName = i;
        while (i < workingContents.length() && workingContents.charAt(i) != stopChar) {
            if (workingContents.charAt(i) == '\n') {
                throw new CompilerException("Incomplete header name: " + context.getCurrentSourcePath() + ":" + i);
            }

            ++i;
        }

        String headerName = startChar + workingContents.substring(startName, i) + stopChar;
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.HEADER_NAME, headerName));

        return i + 1;
    }

    private static int tryGetPPNumber(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) {
        return -1;
    }

    private static int tryGetCharConst(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) {
        if (workingContents.charAt(i) != '\'') {
            return -1;
        }

        int j = i + 1;
        int backslashCount = 0;
        for (; j < workingContents.length(); ++j) {
            if (backslashCount % 2 == 0 && workingContents.charAt(j) == '\'') {
                tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.CHAR_CONST, '\'' + escapeStringLiteral(workingContents.substring(i + 1, j)) + '\''));
                return j + 1;
            }

            if (workingContents.charAt(j) == '\\') {
                ++backslashCount;
            } else {
                backslashCount = 0;
            }
        }

        return -1;
    }

    private static int tryGetStringLiteral(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) {
        if (workingContents.charAt(i) != '"') {
            return -1;
        }

        int j = i + 1;
        int backslashCount = 0;
        for (; j < workingContents.length(); ++j) {
            if (backslashCount % 2 == 0 && workingContents.charAt(j) == '"') {
                tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, '"' + escapeStringLiteral(workingContents.substring(i + 1, j)) + '"'));
                return j + 1;
            }

            if (workingContents.charAt(j) == '\\') {
                ++backslashCount;
            } else {
                backslashCount = 0;
            }
        }

        return -1;
    }

    private static int tryGetOperatorPunctuator(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) {
        if (workingContents.length() > i + 7) {
            String sevenCharsAhead = workingContents.substring(i, i + 7);
            char seventhChar = workingContents.charAt(i + 7);
            if (sevenCharsAhead.contentEquals("defined") && (seventhChar == '(' || Character.isWhitespace(seventhChar))) {
                tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, sevenCharsAhead));
                return i + 7;
            }
        }

        if (workingContents.length() > i + 6) {
            String sixCharsAhead = workingContents.substring(i, i + 6);
            char seventhChar = workingContents.charAt(i + 6);
            if (sixCharsAhead.contentEquals("sizeof") && (seventhChar == '(' || Character.isWhitespace(seventhChar))) {
                tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, sixCharsAhead));
                return i + 6;
            }
        }

        if (workingContents.length() > i + 3) {
            String threeCharsAhead = workingContents.substring(i, i + 3);
            switch (threeCharsAhead) {
                case "<<=", ">>=", "...":
                    tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, threeCharsAhead));
                    return i + 3;
            }
        }

        if (workingContents.length() > i + 2) {
            String twoCharsAhead = workingContents.substring(i, i + 2);
            switch (twoCharsAhead) {
                case "++", "--", "<<", ">>",
                        "<=", ">=", "==", "!=",
                        "&&", "||",
                        "*=", "/=", "%=", "+=", "-=", "&=", "^=", "|=",
                        "->", "##":
                    tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, twoCharsAhead));
                    return i + 2;

                case "//", "/*", "*/":
                    tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.COMMENT, twoCharsAhead));
                    return i + 2;
            }
        }

        if (workingContents.length() > i + 1) {
            char currentChar = workingContents.charAt(i);
            switch (currentChar) {
                case '[', ']', '(', ')', '{', '}', '.',
                        '&', '*', '+', '-', '~', '!', '/', '%',
                        '<', '>', '^', '|',
                        '?', ':', '=', ',', '#', ';':
                    tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, Character.toString(currentChar)));
                    return i + 1;
            }
        }

        return -1;
    }

    private static int tryGetIdentifier(List<PreprocessingToken> tokens, String workingContents, int i, PreprocessingContext context) {
        StringBuilder sb = new StringBuilder();
        char currentChar = workingContents.charAt(i);
        if (!Character.isLetter(currentChar) && currentChar != '_') {
            return -1;
        }
        sb.append(currentChar);

        for (int j = i + 1; j < workingContents.length(); ++j) {
            currentChar = workingContents.charAt(j);
            if (!Character.isLetterOrDigit(currentChar) && currentChar != '_') {
                break;
            }
            sb.append(currentChar);
        }

        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, sb.toString()));
        return i + sb.length();
    }



    public static String escapeStringLiteral(String unescaped) {
        //TODO octal/hexadecimal escapes
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (; i < unescaped.length() - 1; ++i) {
            String nextTwoChars = unescaped.substring(i, i + 2);
            switch (nextTwoChars) {
                case "\\\\":
                    sb.append("\\");
                    ++i;
                    break;

                case "\\\"":
                    sb.append("\"");
                    ++i;
                    break;

                case "\\t":
                    sb.append("\t");
                    ++i;
                    break;

                case "\\'":
                    sb.append("\'");
                    ++i;
                    break;

                case "\\r":
                    sb.append("\r");
                    ++i;
                    break;

                case "\\n":
                    sb.append("\n");
                    ++i;
                    break;

                case "\\f":
                    sb.append("\f");
                    ++i;
                    break;

                case "\\b":
                    sb.append("\b");
                    ++i;
                    break;

                default:
                    sb.append(unescaped.charAt(i));
            }
        }
        if (unescaped.length() >= 2 && i == unescaped.length() - 1) {
            sb.append(unescaped.charAt(unescaped.length() - 1));
        }

        return sb.toString();
    }

    public static String inverseEscapeStringLiteral(String escaped) {
        //TODO octal/hexadecimal escapes
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < escaped.length() - 1; ++i) {
            char nextChar = escaped.charAt(i);
            switch (nextChar) {
                case '\\':
                    sb.append("\\\\");
                    break;

                case '\"':
                    sb.append("\\\"");
                    break;

                case '\t':
                    sb.append("\\t");
                    break;

                case '\'':
                    sb.append("\\\'");
                    break;

                case '\r':
                    sb.append("\\r");
                    break;

                case '\n':
                    sb.append("\\n");
                    break;

                case '\f':
                    sb.append("\\f");
                    break;

                case '\b':
                    sb.append("\\b");
                    break;

                default:
                    sb.append(nextChar);
            }
        }

        return escaped.charAt(0) + sb.toString() + escaped.charAt(escaped.length() - 1);
    }
}
