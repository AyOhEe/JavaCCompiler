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
    private void constructTimeMacro(LocalDateTime compilationStart) {
        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__TIME__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, formatTime(compilationStart)));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));

        defineObjectlike(tokens, 0);
    }
    private void constructDateMacro(LocalDateTime compilationStart) {
        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__DATE__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.STRING_LIT, formatDate(compilationStart)));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));

        defineObjectlike(tokens, 0);
    }
    private void constructSTDCMacro() {
        List<PreprocessingToken> tokens = new ArrayList<>();
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "__STDC__"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.PP_NUMBER, "1"));
        tokens.add(new PreprocessingToken(PreprocessingToken.TokenType.NEWLINE, "\n"));

        defineObjectlike(tokens, 0);
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

    //TODO update
    /*public void doReplacement(List<String> lines, int i) throws CompilerException {
        boolean wasUpdated = true;
        int depth = 0;
        while (wasUpdated) {
            depth++;
            if (depth > REPLACEMENT_LIMIT) {
                throw new CompilerException("Maximum replacement depth reached");
            }
            String initialLine = lines.get(i);

            for(Map.Entry<String, PreprocessorDefinition> entry : macros.entrySet()) {
                entry.getValue().replaceInstances(entry.getKey(), lines, i, verbose);
            }

            wasUpdated = !initialLine.contentEquals(lines.get(i));
        }
    }
    public String doReplacement(String line) throws CompilerException {
        List<String> lines = new ArrayList<>();
        lines.add(line);

        doReplacement(lines, 0);

        return lines.getFirst();
    }*/

    public void fileDeeper(Path nextFile) throws CompilerException {
        fileStack.push(nextFile);
        //TODO update
        //macros.put("__FILE__", PreprocessorDefinition.parse("__FILE__ " + escapePath(fileStack.peek()) + "\n", 0));

        if (fileStack.size() > MAX_FILE_DEPTH) {
            throw new CompilerException("Maximum #include depth reached");
        }
    }
    public void fileOut() throws CompilerException {
        fileStack.pop();
        //TODO update
        //String macro = fileStack.empty() ? "\"UNKNOWN\"\n" : escapePath(fileStack.peek()) + "\n";
        //macros.put("__FILE__", PreprocessorDefinition.parse("__FILE__ " + macro, 0));
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

    public int defineObjectlike(List<PreprocessingToken> tokens, int i) {
        //TODO this
        while (!tokens.get(i).is(PreprocessingToken.TokenType.NEWLINE)) {
            tokens.remove(i);
        }
        return i;
    }

    public int defineFunctionlike(List<PreprocessingToken> tokens, int i) {
        //TODO this
        while (!tokens.get(i).is(PreprocessingToken.TokenType.NEWLINE)) {
            tokens.remove(i);
        }
        return i;
    }

    public void undefine(String name) {
        macros.remove(name);
    }
}
