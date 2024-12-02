package ayohee.c_compiler;

import java.util.HashMap;
import java.util.Map;

public class PreprocessingContext {
    private HashMap<String, PreprocessorDefinition> macros = new HashMap<>();

    public PreprocessingContext() { }

    public boolean isDefined(String identifier) {
        return macros.containsKey(identifier);
    }

    public void define(String identifier, String value) {
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
}
