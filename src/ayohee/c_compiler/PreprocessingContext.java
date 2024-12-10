package ayohee.c_compiler;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

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
        macros.put("__TIME__", PreprocessorDefinition.parse("__TIME__ " + formatTime(compilationStart) + "\n", 0));
        macros.put("__DATE__", PreprocessorDefinition.parse("__DATE__ " + formatDate(compilationStart) + "\n", 0));
        macros.put("__STDC__", PreprocessorDefinition.parse("__STDC__ 1" + "\n", 0));
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

    private static String escapePath(Path sourcePath) {
        return "\"" + sourcePath.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public boolean isDefined(String identifier) {
        return macros.containsKey(identifier);
    }

    public void define(String statement) throws CompilerException {
        String identifier = PreprocessorDefinition.findIdentifier(statement, 7);
        if (Preprocessor.isValidIdentifier(identifier)) {
            macros.put(identifier, PreprocessorDefinition.parse(statement, 7));
        } else {
            throw new CompilerException("Attempted to define invalid identifier \"" + identifier + "\"" + " on line: " + statement);
        }
    }

    public void undefine(String identifier) {
        macros.remove(identifier);
    }

    public String doReplacement(String line) throws CompilerException {
        boolean wasUpdated = true;
        int depth = 0;
        while (wasUpdated) {
            depth++;
            if (depth > REPLACEMENT_LIMIT) {
                throw new CompilerException("Maximum replacement depth reached");
            }
            String initialLine = line;

            for(Map.Entry<String, PreprocessorDefinition> entry : macros.entrySet()) {
                line = entry.getValue().replaceInstances(entry.getKey(), line, verbose);
            }
            line = handleDoublehashConcatenation(line);

            wasUpdated = !initialLine.contentEquals(line);
        }
        line = evaluateConstexprs(line);
        return line;
    }

    private String handleDoublehashConcatenation(String line) {
        //TODO this
        return line;
    }

    public String evaluateConstexprs(String expression) {
        //TODO this
        return expression;
    }

    public void fileDeeper(Path nextFile) throws CompilerException {
        fileStack.push(nextFile);
        macros.put("__FILE__", PreprocessorDefinition.parse("__FILE__ " + escapePath(fileStack.peek()) + "\n", 0));

        if (fileStack.size() > MAX_FILE_DEPTH) {
            throw new CompilerException("Maximum #include depth reached");
        }
    }
    public void fileOut() throws CompilerException {
        fileStack.pop();
        String macro = fileStack.empty() ? "\"UNKNOWN\"\n" : escapePath(fileStack.peek()) + "\n";
        macros.put("__FILE__", PreprocessorDefinition.parse("__FILE__ " + macro, 0));
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
}
