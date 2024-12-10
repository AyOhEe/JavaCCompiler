package ayohee.c_compiler;

import java.util.List;

public class ObjectLikePreprocessorDefinition extends PreprocessorDefinition {
    private String replacementList;

    public ObjectLikePreprocessorDefinition(String line, int firstWhitespace) {
        if (firstWhitespace == line.length() - 1) {
            //empty definitions default to 1
            this.replacementList = "1";
            return;
        }

        //pad on the right with a single space, and trim the newline
        this.replacementList = line.substring(firstWhitespace + 1, line.length() - 1);
    }

    @Override
    public void replaceInstances(String label, List<String> lines, int i, boolean verbose) {
        lines.set(i, lines.get(i).replaceAll("\\b" + label + "\\b", replacementList));
    }

    @Override
    public String toString() {
        return replacementList;
    }
}
