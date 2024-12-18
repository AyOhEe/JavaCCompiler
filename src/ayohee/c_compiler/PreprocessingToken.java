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
        COMMENT,
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
}
