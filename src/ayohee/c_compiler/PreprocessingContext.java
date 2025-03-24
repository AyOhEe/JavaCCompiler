package ayohee.c_compiler;

import java.nio.file.Path;
import java.sql.Array;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

public class PreprocessingContext {
    private final int MAX_FILE_DEPTH = 32;
    private final int REPLACEMENT_LIMIT = 16;


    private HashMap<String, PreprocessorDefinition> macros = new HashMap<>();
    private Stack<Path> fileStack;
    private Path originalSourcePath;
    private boolean yesMode;
    private boolean verbose;

    private int lineNumber;

    public PreprocessingContext(Path originalSourcePath, LocalDateTime compilationStart, boolean yesMode, boolean verbose) throws CompilerException {
        this.fileStack = new Stack<>();
        this.originalSourcePath = originalSourcePath;
        this.yesMode = yesMode;
        this.verbose = verbose;
        this.lineNumber = 1;

        //don't do this through define or it'll throw for re-defining predefined macros
        //__FILE__ and __LINE__ are handled as special cases, as they are file dependant
        constructTimeMacro(compilationStart);
        constructDateMacro(compilationStart);
        constructSTDCMacro();
        updateLineMacro();
    }
    private void constructTimeMacro(LocalDateTime compilationStart) throws CompilerException {
        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__TIME__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, formatTime(compilationStart)));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));

        defineObjectlike(tokens, 0, this, true);
    }
    private void constructDateMacro(LocalDateTime compilationStart) throws CompilerException {
        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__DATE__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, formatDate(compilationStart)));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));

        defineObjectlike(tokens, 0, this, true);
    }
    private void constructSTDCMacro() throws CompilerException {
        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__STDC__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.PP_NUMBER, "1"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));

        defineObjectlike(tokens, 0, this, true);
    }

    private void updateLineMacro() throws CompilerException {
        macros.remove("__LINE__");

        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__LINE__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.PP_NUMBER, Integer.toString(lineNumber)));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));
        defineObjectlike(tokens, 0, this, true);
    }

    private static String formatDate(LocalDateTime compilationStart) {
        return String.format("\"%s %2d %d\"", monthAsString(compilationStart.getMonth()), compilationStart.getDayOfMonth(), compilationStart.getYear());
    }

    private static String monthAsString(Month month) {
        return switch(month) {
            case JANUARY -> "Jan";
            case FEBRUARY -> "Feb";
            case MARCH -> "Mar";
            case APRIL -> "Apr";
            case MAY -> "May";
            case JUNE -> "Jun";
            case JULY -> "Jul";
            case AUGUST -> "Aug";
            case SEPTEMBER -> "Sep";
            case OCTOBER -> "Oct";
            case NOVEMBER -> "Nov";
            case DECEMBER -> "Dec";
        };
    }

    private static String formatTime(LocalDateTime compilationStart) {
        return String.format("\"%02d:%02d:%02d\"", compilationStart.getHour(), compilationStart.getMinute(), compilationStart.getSecond());
    }

    public void doReplacement(List<PreprocessingToken> tokens, int i, boolean singleToken) throws CompilerException {
        boolean wasUpdated = true;
        int depth = 0;
        while (wasUpdated) {
            depth++;
            if (depth > REPLACEMENT_LIMIT) {
                throw new CompilerException(this, "Maximum replacement depth reached");
            }

            wasUpdated = false;
            for (int j = i; (singleToken && j < i + 1) && !tokens.get(j).is(PreprocessingToken.TokenType.NEWLINE); ++j) {
                wasUpdated |= replaceDefinitionCheck(tokens, j);

                for(Map.Entry<String, PreprocessorDefinition> entry : macros.entrySet()) {
                    boolean didReplacement = entry.getValue().replaceInstances(entry.getKey(), tokens, j);
                    wasUpdated |= didReplacement;
                }
            }

        }
    }

    private boolean replaceDefinitionCheck(List<PreprocessingToken> tokens, int i) {
        if (!tokens.get(i).is("defined") || !tokens.get(i + 1).is("(")
            || !tokens.get(i + 2).is(PreprocessingToken.TokenType.IDENTIFIER) || !tokens.get(i + 3).is(")")) {
            return false;
        }

        tokens.remove(i); //defined
        tokens.remove(i); //(
        PreprocessingToken name = tokens.remove(i); //identifier
        tokens.remove(i); //)

        if (macros.containsKey(name.toString())) {
            tokens.add(i, new PreprocessingToken(PreprocessingToken.TokenType.PP_NUMBER, "1"));
        } else {
            tokens.add(i, new PreprocessingToken(PreprocessingToken.TokenType.PP_NUMBER, "0"));
        }

        return true;
    }

    public void fileDeeper(Path nextFile) throws CompilerException {
        fileStack.push(nextFile);

        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__FILE__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, fileStack.peek().toString()));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));
        defineObjectlike(tokens, 0, this, true);

        if (fileStack.size() > MAX_FILE_DEPTH) {
            throw new CompilerException(this, "Maximum #include depth reached");
        }
    }
    public void fileOut() throws CompilerException {
        fileStack.pop();
        String name = fileStack.empty() ? "\"UNKNOWN\"" : fileStack.peek().toString();

        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__FILE__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, name));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));
        defineObjectlike(tokens, 0, this, true);
    }

    public Path getOriginalSourcePath() {
        return originalSourcePath;
    }

    public Path getCurrentSourcePath() {
        return fileStack.peek();
    }

    public boolean isVerbose() {
        return verbose;
    }
    public boolean isYesMode() {
        return yesMode;
    }

    public int defineObjectlike(List<PreprocessingToken> tokens, int i, PreprocessingContext context) throws CompilerException {
        return defineObjectlike(tokens, i, context, false);
    }
    private int defineObjectlike(List<PreprocessingToken> tokens, int i, PreprocessingContext context, boolean force) throws CompilerException {
        PreprocessingToken label = tokens.remove(i);

        List<PreprocessingToken> replacementList = new ArrayList<>();
        while (!tokens.get(i).is(PreprocessingToken.TokenType.NEWLINE)) {
            replacementList.add(tokens.remove(i));
        }

        if (label.is(PreprocessingToken.TokenType.IDENTIFIER) && (force || Preprocessor.isValidIdentifier(label.toString(), context))) {
            macros.put(label.toString(), new ObjectLikePreprocessorDefinition(replacementList));
            return i;
        } else {
            throw new CompilerException(this, "Tried to define macro with invalid name: \"" + label.toString() + "\"" + getCurrentSourcePath());
        }
    }

    public int defineFunctionlike(List<PreprocessingToken> tokens, int i, PreprocessingContext context) throws CompilerException {
        List<PreprocessingToken> statement = new ArrayList<>();
        while (!tokens.get(i).is(PreprocessingToken.TokenType.NEWLINE)) {
            statement.add(tokens.remove(i));
        }

        String label = statement.getFirst().toString();

        if (statement.getFirst().is(PreprocessingToken.TokenType.FUNCTIONLIKE_MACRO_DEFINITION) && Preprocessor.isValidIdentifier(label, this)) {
            macros.put(label, new FunctionLikePreprocessorDefinition(statement, context));
            return i;
        } else {
            throw new CompilerException(this, "Tried to define macro with invalid name \"" + label + "\"" + getCurrentSourcePath());
        }
    }

    public void undefine(String name) throws CompilerException {
        if (Preprocessor.isValidIdentifier(name, this)) {
            macros.remove(name);
        } else {
            throw new CompilerException(this, "Tried to undefine macro with invalid name \"" + name + "\"" + getCurrentSourcePath());
        }
    }


    public void setLineNumber(int line) throws CompilerException {
        lineNumber = line;
        updateLineMacro();
    }

    public void incrementLineNumber() throws CompilerException {
        lineNumber += 1;
        updateLineMacro();
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
