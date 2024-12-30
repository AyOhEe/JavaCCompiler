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

    public PreprocessingContext(Path originalSourcePath, LocalDateTime compilationStart, boolean yesMode, boolean verbose) throws CompilerException {
        this.fileStack = new Stack<>();
        this.originalSourcePath = originalSourcePath;
        this.yesMode = yesMode;
        this.verbose = verbose;

        //don't do this through define or it'll throw for re-defining predefined macros
        //__FILE__ and __LINE__ are handled as special cases, as they are file dependant
        constructTimeMacro(compilationStart);
        constructDateMacro(compilationStart);
        constructSTDCMacro();
    }
    private void constructTimeMacro(LocalDateTime compilationStart) throws CompilerException {
        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__TIME__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, formatTime(compilationStart)));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));

        defineObjectlike(tokens, 0, true);
    }
    private void constructDateMacro(LocalDateTime compilationStart) throws CompilerException {
        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__DATE__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, formatDate(compilationStart)));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));

        defineObjectlike(tokens, 0, true);
    }
    private void constructSTDCMacro() throws CompilerException {
        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__STDC__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.PP_NUMBER, "1"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));

        defineObjectlike(tokens, 0, true);
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

    public void doReplacement(List<PreprocessingToken> tokens, int i) throws CompilerException {
        boolean wasUpdated = true;
        int depth = 0;
        while (wasUpdated) {
            depth++;
            if (depth > REPLACEMENT_LIMIT) {
                throw new CompilerException("Maximum replacement depth reached");
            }
            PreprocessingToken initialToken = tokens.get(i);

            for(Map.Entry<String, PreprocessorDefinition> entry : macros.entrySet()) {
                entry.getValue().replaceInstances(entry.getKey(), tokens, i);
            }

            wasUpdated = !initialToken.toString().contentEquals(tokens.get(i).toString());
        }
    }

    public void fileDeeper(Path nextFile) throws CompilerException {
        fileStack.push(nextFile);

        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__FILE__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, fileStack.peek().toString()));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));
        defineObjectlike(tokens, 0, true);

        if (fileStack.size() > MAX_FILE_DEPTH) {
            throw new CompilerException("Maximum #include depth reached");
        }
    }
    public void fileOut() throws CompilerException {
        fileStack.pop();
        String name = fileStack.empty() ? "\"UNKNOWN\"" : fileStack.peek().toString();

        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__FILE__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, name));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));
        defineObjectlike(tokens, 0, true);
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

    public int defineObjectlike(List<PreprocessingToken> tokens, int i) throws CompilerException {
        return defineObjectlike(tokens, i, false);
    }
    private int defineObjectlike(List<PreprocessingToken> tokens, int i, boolean force) throws CompilerException {
        PreprocessingToken label = tokens.remove(i);

        List<PreprocessingToken> replacementList = new ArrayList<>();
        while (!tokens.get(i).is(PreprocessingToken.TokenType.NEWLINE)) {
            replacementList.add(tokens.remove(i));
        }

        if (label.is(PreprocessingToken.TokenType.IDENTIFIER) && (force || Preprocessor.isValidIdentifier(label.toString()))) {
            macros.put(label.toString(), new ObjectLikePreprocessorDefinition(replacementList));
            return i;
        } else {
            throw new CompilerException("Tried to define macro with invalid name: \"" + label.toString() + "\"" + getCurrentSourcePath());
        }
    }

    public int defineFunctionlike(List<PreprocessingToken> tokens, int i) throws CompilerException {
        List<PreprocessingToken> statement = new ArrayList<>();
        while (!tokens.get(i).is(PreprocessingToken.TokenType.NEWLINE)) {
            statement.add(tokens.remove(i));
        }

        String label = statement.getFirst().toString();
        label = label.substring(0, label.length() - 1);

        if (statement.getFirst().is(PreprocessingToken.TokenType.FUNCTIONLIKE_MACRO_DEFINITION) && Preprocessor.isValidIdentifier(label)) {
            macros.put(label, new FunctionLikePreprocessorDefinition(statement));
            return i;
        } else {
            throw new CompilerException("Tried to define macro with invalid name \"" + label + "\"" + getCurrentSourcePath());
        }
    }

    public void undefine(String name) {
        macros.remove(name);
    }
}
