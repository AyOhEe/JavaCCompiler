package ayohee.c_compiler;

import java.util.ArrayList;

public class FunctionLikePreprocessorDefinition extends PreprocessorDefinition {
    ArrayList<String> argumentNames;
    String replacementList;

    public FunctionLikePreprocessorDefinition(String line, int lParenIndex) throws CompilerException {
        argumentNames = new ArrayList<>();
        int i = lParenIndex + 1;
        while (i < line.length()) {
            //move to the identifier if we're not at it
            i = skipWhitespace(line, i);
            i = addNextIdentifier(line, i, argumentNames);

            //if that's the end, skipping the "comma" would actually just skip the rparen
            if (line.charAt(i) == ')') {
                break;
            }

            //skip the comma and following whitespace
            i = skipWhitespace(line, i + 1);
        }
        replacementList = line.substring(i + 1, line.length() - 1);
    }

    private int addNextIdentifier(String line, int i, ArrayList<String> argumentNames) throws CompilerException {
        int j = i;
        while (j < line.length() && line.charAt(j) != ',' && line.charAt(j) != ')') {
            ++j;
        }

        if (j == line.length()) {
            throw new CompilerException("Unclosed function-like macro definition: " + line);
        }

        //if the last argument in the list has whitespace padding on the right, it'll be regarded as invalid.
        //get rid of any trailing whitespace
        String identifier = line.substring(i, j).stripTrailing();
        if (!Preprocessor.isValidIdentifier(identifier)) {
            throw new CompilerException("Invalid identifier \"" + identifier + "\" in function-like macro definition: " + line);
        }


        argumentNames.add(identifier);
        return j;
    }

    private int skipWhitespace(String line, int i) {
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            ++i;
        }
        return i;
    }

    @Override
    public String replaceInstances(String label, String line, boolean verbose) {
        //TODO this
        return line;
    }

    @Override
    public String toString() {
        return argumentNames.toString() + ": " + replacementList;
    }
}
