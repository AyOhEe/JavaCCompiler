package ayohee.c_compiler;

public class PreprocessingToken {
    public enum TokenType {
        HEADER_NAME,
        IDENTIFIER,
        PP_NUMBER,
        CHAR_CONST,
        STRING_LIT,
        OPERATOR_PUNCTUATOR,
        NEWLINE,
        OTHER
    }

    private TokenType type;
    private String asString;

    public PreprocessingToken(TokenType type, String contents) {
        this.type = type;
        this.asString = contents;
    }


    public TokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        return switch (type) {
            case STRING_LIT -> '"' + Tokenizer.inverseEscapeStringLiteral(asString) + '"';
            case CHAR_CONST -> '\'' + Tokenizer.inverseEscapeStringLiteral(asString) + '\'';
            default -> asString;
        };
    }

    public String unescapedString() {
        return asString;
    }

    public boolean is(String match) {
        return match.contentEquals(asString);
    }
    public boolean is(TokenType match) {
        return match == type;
    }
}
