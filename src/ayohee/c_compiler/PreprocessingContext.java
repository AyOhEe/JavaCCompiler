package ayohee.c_compiler;

import java.util.HashMap;

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

    public String doReplacement(String line) {
        //TODO
        return line;
    }
}
