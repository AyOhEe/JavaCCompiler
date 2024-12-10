package ayohee.c_compiler;

import java.util.ArrayList;
import java.util.List;

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
    public void replaceInstances(String label, List<String> lines, int i, boolean verbose) throws CompilerException {
        if (!lines.get(i).contains(label)) {
            return;
        }

        List<String>[] arguments = extractArgumentsAndRemoveCall(label, lines, i, verbose);
        List<String> replacementResult = new ArrayList<String>();
        replacementResult.add(replacementList);

        for (int argIndex = 0; argIndex < argumentNames.size(); ++argIndex) {
            String argumentName = argumentNames.get(argIndex);
            List<String> argumentLines = arguments[argIndex];
            replacementResult = replaceWithLines(replacementResult, argumentName, argumentLines);
        }

        replaceFirstInvokation(label, lines, i, replacementResult, verbose);
    }

    private void replaceFirstInvokation(String label, List<String> lines, int i, List<String> replacementResult, boolean verbose) {
        //TODO this
    }

    private List<String> replaceWithLines(List<String> initialReplacement, String argumentName, List<String> argumentLines) {
        //TODO this
        return initialReplacement;
    }

    private List<String>[] extractArgumentsAndRemoveCall(String label, List<String> lines, int i, boolean verbose) throws CompilerException {
        List<String>[] arguments = new List[argumentNames.size()];
        int startFrom = lines.get(i).indexOf(label) + label.length(); //first character after the label

        int j = i;
        int k = startFrom;

        String atJ = lines.get(j);
        while (atJ.charAt(k) != '(') {
            if (!Character.isWhitespace(atJ.charAt(k))) {
                throw new CompilerException("Function-like macro was referenced but never invoked: " + lines.get(i));
            }

            ++k;
            if (k == atJ.length()) {
                j += 1;
                k = 0;
            }
            atJ = lines.get(j);
        }

        ++k; //skip the lparen
        int closeParenLineIndex = -1;
        int closeParenCharIndex = -1;
        for (int argIndex = 0; argIndex < argumentNames.size(); ++argIndex) {
            int argumentStartLine = j;
            int argumentStartChar = k;
            boolean inString = false;
            boolean inChar = false;
            int parenDepth = 0;
            int backslashCount = 0;
            while (!(parenDepth == 0 && atJ.charAt(k) == ',') && !inString && !inChar) {
                char atK = atJ.charAt(k);
                if ((inString || inChar) && atK == '\\') {
                    backslashCount += 1;
                }

                if (parenDepth == 0 && (atK == ',' || atK == ')')) {
                    if (atK == ')') {
                        closeParenLineIndex = j;
                        closeParenCharIndex = k;
                    }
                    break;
                }

                if (!inChar && !inString && atK == '"') {
                    inString = true;
                }
                if (inString && backslashCount % 2 == 0 && atK == '"') {
                    inString = false;
                }
                if (!inString && !inChar && atK == '\'') {
                    inChar = true;
                }
                if (inChar && backslashCount % 2 == 0 && atK == '\'') {
                    inChar = false;
                }

                if (!inString && !inChar && atK == '(') {
                    parenDepth += 1;
                }
                if (!inString && !inChar && atK == ')') {
                    parenDepth -= 1;
                }

                if (parenDepth < 0) {
                    throw new CompilerException("Function-like macro provided too few arguments: " + lines.get(i));
                }

                if (atK != '\\') {
                    backslashCount = 0;
                }

                ++k;
                if (k == atJ.length()) {
                    j += 1;
                    k = 0;
                }
                if (j == lines.size()) {
                    throw new CompilerException("Function-like macro unclosed - continued to EOF: " + lines.get(i));
                }
                atJ = lines.get(j);
            }

            arguments[argIndex] = multilineSubstring(lines, argumentStartLine, argumentStartChar, j, k);
            ++k; //skip the comma if it's there
        }

        if (closeParenCharIndex == -1) {
            throw new CompilerException("Function-like macro provided too many arguments: " + lines.get(i));
        }

        for(List<String> argument : arguments) {
            if (argument == null) {
                throw new CompilerException("Function-like macro provided too few arguments: " + lines.get(i));
            }
        }

        return arguments;
    }

    private List<String> multilineSubstring(List<String> lines, int argumentStartLine, int argumentStartChar, int argumentEndLine, int argumentEndChar) {
        //TODO this but actually
        List<String> substr = new ArrayList<String>();
        substr.add(lines.get(argumentStartLine).substring(argumentStartChar, argumentEndChar));
        return substr;
    }

    @Override
    public String toString() {
        return argumentNames.toString() + ": " + replacementList;
    }
}
