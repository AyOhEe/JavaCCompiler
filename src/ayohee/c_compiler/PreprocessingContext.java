package ayohee.c_compiler;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;

public class PreprocessingContext {
    private final int MAX_FILE_DEPTH = 32;


    private HashMap<String, PreprocessorDefinition> macros = new HashMap<>();
    private Path sourcePath = null;
    private int fileDepth = 0;
    private boolean yesMode;
    private boolean verbose;

    public PreprocessingContext(Path sourcePath, LocalDateTime compilationStart, boolean yesMode, boolean verbose) {
        this.sourcePath = sourcePath;
        this.yesMode = yesMode;
        this.verbose = verbose;

        //don't do this through define or it'll throw for re-defining predefined macros
        macros.put("__FILE__", PreprocessorDefinition.parse(escapePath(sourcePath)));
        macros.put("__TIME__", PreprocessorDefinition.parse(formatTime(compilationStart)));
        macros.put("__DATE__", PreprocessorDefinition.parse(formatDate(compilationStart)));
        macros.put("__STDC__", PreprocessorDefinition.parse("1"));
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

    public void define(String identifier, String value) throws CompilerException {
        switch (identifier) {
            case "__LINE__", "__FILE__", "__DATE__", "__TIME__", "__STDC__" -> throw new CompilerException("Attempted to define predefined macro: " + identifier);
        }
        macros.put(identifier, PreprocessorDefinition.parse(value));
    }

    public void undefine(String identifier) {
        macros.remove(identifier);
    }

    public String doReplacement(String line, boolean verbose) {
        for(Map.Entry<String, PreprocessorDefinition> entry : macros.entrySet()) {
            line = entry.getValue().replaceInstances(entry.getKey(), line, verbose);
        }
        line = evaluateConstexprs(line);
        return line;
    }

    public String evaluateConstexprs(String expression) {
        //TODO this
        return expression;
    }

    public void fileDeeper() throws CompilerException {
        fileDepth += 1;
        if (fileDepth > MAX_FILE_DEPTH) {
            throw new CompilerException("Maximum #include depth reached");
        }
    }
    public void fileOut() {
        fileDepth -= 1;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public boolean isVerbose() {
        return verbose;
    }
    public boolean isYesMode() {
        return yesMode;
    }
}
