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
        if (type != TokenType.STRING_LIT && type != TokenType.CHAR_CONST) {
            return asString;
        }
        else {
            return Tokenizer.inverseEscapeStringLiteral(asString);
        }
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
