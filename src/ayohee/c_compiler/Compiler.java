package ayohee.c_compiler;

import java.nio.file.Path;
import java.util.ArrayList;

public class Compiler {
    public static void compile(ArrayList<Path> sourceFiles, ArrayList<Path> includePaths, Path asmOutputPath, boolean verbose, boolean yesMode) {
        //TODO this
        ArrayList<String> compilationUnits = Preprocessor.preprocess(sourceFiles, includePaths, yesMode, verbose);
    }
}
