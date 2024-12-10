package ayohee.c_compiler;

import java.util.List;

public abstract class PreprocessorDefinition {
    public static PreprocessorDefinition parse(String line, int startAt) throws CompilerException {
        int identifierBegins = -1;
        for (int i = startAt; i < line.length(); ++i) {
            if (!Character.isWhitespace(line.charAt(i))) {
                identifierBegins = i;
                break;
            }
        }
        if (identifierBegins == -1) {
            throw new CompilerException("No identifier in line after startAt");
        }

        int identifierEnds = -1;
        for(int i = identifierBegins; i < line.length(); ++i) {
            if (Character.isWhitespace(line.charAt(i)) || line.charAt(i) == '(') {
                identifierEnds = i - 1;
                break;
            }
        }
        if (identifierEnds == -1) {
            throw new CompilerException("This shouldn't happen. PreprocessorDefinition.java::parse");
        }

        if (line.charAt(identifierEnds + 1) == '(') {
            return new FunctionLikePreprocessorDefinition(line, identifierEnds + 1);
        } else {
            //from the first whitespace after the identifier, trimming the newline
            return new ObjectLikePreprocessorDefinition(line, identifierEnds + 1);
        }
    }

    public static String findIdentifier(String line, int startAt) throws CompilerException {
        int identifierBegins = -1;
        for (int i = startAt; i < line.length(); ++i) {
            if (!Character.isWhitespace(line.charAt(i))) {
                identifierBegins = i;
                break;
            }
        }

        if (identifierBegins == -1) {
            throw new CompilerException("No identifier in line after startAt");
        }

        for(int i = identifierBegins; i < line.length(); ++i) {
            if (!Character.isWhitespace(line.charAt(i)) && line.charAt(i) != '(') {
                continue;
            }
            return line.substring(identifierBegins, i);
        }
        throw new CompilerException("This shouldn't happen. PreprocessorDefinition.java::findIdentifier");
    }

    public abstract void replaceInstances(String label, List<String> lines, int i, boolean verbose) throws CompilerException;
}
