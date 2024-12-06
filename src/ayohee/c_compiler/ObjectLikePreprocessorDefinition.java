package ayohee.c_compiler;

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
    public String replaceInstances(String label, String line, boolean verbose) {
        return line.replaceAll("\\b" + label + "\\b", replacementList);
    }

    @Override
    public String toString() {
        return replacementList;
    }
}
