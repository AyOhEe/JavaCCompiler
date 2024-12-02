package ayohee.c_compiler;

public class PreprocessorDefinition {
    public static PreprocessorDefinition parse(String value) {
        return new PreprocessorDefinition(value);
    }

    public static String findIdentifier(String line, int startAt) throws CompilerException {
        int identifierBegins = -1;
        for (int i = startAt; i < line.length(); ++i) {
            if (Character.isWhitespace(line.charAt(i))) {
                continue;
            }
            identifierBegins = i;
            break;
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

    public static String extractReplacementList(String trimmed) {
        //TODO constexpr evaluations
        //TODO this
        return "DOES NOT WORK";
    }

    String replacementList;

    public PreprocessorDefinition(String replacement) {
        replacementList = replacement.isBlank() ? "1" : replacement;
    }


    public String replaceInstances(String label, String line, boolean verbose) {
        //TODO does not work for function-like macros
        return line.replaceAll("\\b" + label + "\\b", replacementList);
    }
}
