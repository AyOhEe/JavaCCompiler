import ayohee.c_compiler.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws CompilerException {
        if (args.length == 0){
            System.out.println("Java C Compiler: No arguments supplied. Use -h or --help for more information.");
            System.exit(-1);
        }

        //command line flags
        boolean compile = true;
        boolean assemble = true;
        boolean link = true;
        boolean yesMode = false;
        boolean verbose = false;
        boolean cleanup = true;

        //command line arguments
        ArrayList<Path> includePaths = new ArrayList<>();
        ArrayList<Path> libraryPaths = new ArrayList<>();
        ArrayList<Path> sourcePaths = new ArrayList<>();
        Path linkerOutputName = Path.of("main").toAbsolutePath();
        Path ppOutputPath = Path.of("out_pp/").toAbsolutePath();
        Path asmOutputPath = Path.of("out_asm/").toAbsolutePath();
        Path objOutputPath = Path.of("out_obj/").toAbsolutePath();

        //parse command line arguments
        for (int i = 0; i < args.length; i++){
            switch (args[i]){
                case "-h", "--help":
                    showHelp();
                    System.exit(0);
                    break;

                case "-v", "--verbose":
                    verbose = true;
                    break;

                case "-p", "--preprocess-only":
                    compile = false;
                    assemble = false;
                    link = false;
                    break;

                case "-c", "--compilation-only":
                    assemble = false;
                    link = false;
                    break;

                case "-a", "--assemble-only":
                    link = false;
                    break;

                case "-y", "--yes":
                    yesMode = true;
                    System.out.println("WARNING: Running in yes mode. Make sure you know what you're doing.");
                    break;

                case "-nc", "--no-cleanup":
                    cleanup = false;
                    break;
            }

            //if we're on the last argument, don't process arguments that expect another argument to follow
            if(i == args.length - 1) {
                break;
            }
            switch (args[i]){
                case "-s", "--source" -> sourcePaths.add(Path.of(args[i + 1]).toAbsolutePath());
                case "-i", "--include" -> includePaths.add(Path.of(args[i + 1]).toAbsolutePath());
                case "-l", "--library" -> libraryPaths.add(Path.of(args[i + 1]).toAbsolutePath());
                case "-po", "--preprocessor-output" -> ppOutputPath = Path.of(args[i + 1]).toAbsolutePath();
                case "-co", "--compiler-output" -> asmOutputPath = Path.of(args[i + 1]).toAbsolutePath();
                case "-ao", "--assembler-output" -> objOutputPath = Path.of(args[i + 1]).toAbsolutePath();
                case "-o", "-lo", "--linker-output" -> linkerOutputName = Path.of(args[i + 1]).toAbsolutePath();
            }
        }

        System.out.println("Verbose mode: " + (verbose ? "enabled" : "disabled"));
        if (verbose) {
            System.out.println("Yes mode: " + (yesMode ? "enabled" : "disabled"));
            System.out.println("Will compile: " + (compile ? "yes" : "no"));
            System.out.println("Will assemble: " + (assemble ? "yes" : "no"));
            System.out.println("Will link: " + (link ? "yes" : "no"));
            System.out.println("Will clean up intermediary files: " + (cleanup ? "yes" : "no") + "\n");
        }


        //search through each source directory and store each .c file found
        ArrayList<Path> sourceFiles = new ArrayList<>();
        for (Path path : sourcePaths){
            try{
                System.out.println("Searching " + path.toString() + " for source files...");
                sourceFiles.addAll(detectFiles(path, ".*[.]c"));   //*.c files in this directory or any subdirectory
            }
            catch (IOException e){
                System.out.println("Failed to detect source files in " + path);
            }
        }
        if (sourceFiles.isEmpty()) {
            System.out.println("No source files detected to compile.");
            System.exit(1);
        }

        if (verbose){
            System.out.println("Detected source files: ");
            for (Path path : sourceFiles){
                System.out.println("\t" + path.toString());
            }

            if (assemble) {
                System.out.println("\nDetected include paths: ");
                for (Path path : includePaths) {
                    System.out.println("\t" + path.toString());
                }
            } else {
                System.out.println("\nDetected include paths: Disabled");
            }

            if (assemble && link){
                System.out.println("\nDetected library paths: ");
                for (Path path : libraryPaths){
                    System.out.println("\t" + path.toString());
                }
            } else {
                System.out.println("\nDetected library paths: Disabled");
            }

            System.out.println("\nPreprocessor output directory: " + ppOutputPath);
            System.out.println("Compiler output directory: " + (compile ? asmOutputPath : "Disabled"));
            System.out.println("Assembler output directory: " + (assemble ? objOutputPath : "Disabled"));
            System.out.println("Linker output path: " + (link ? linkerOutputName : "Disabled") + "\n");
        }


        //preprocess to compilation units
        //PREPROCESSING
        String msg = "Preprocessing will delete all files and directories in " + ppOutputPath + ". Are you sure? (y/n)";
        ArrayList<Path> ppuFiles;
        if(cleanup || confirmUserIntent(msg, yesMode)) {
            refreshPath(ppOutputPath, "Unable to refresh preprocessor output path at " + ppOutputPath);
            ppuFiles = Preprocessor.preprocess(sourceFiles, includePaths, ppOutputPath, yesMode, verbose);
            System.out.println("Preprocessing successfully finished.");
        } else {
            System.out.println("Preprocessing aborted");
            System.exit(1);
            return;
        }

        //compile to assembly
        //COMPILATION
        if (compile) {
            msg = "Compilation will delete all files and directories in " + asmOutputPath + ". Are you sure? (y/n)";
            if(cleanup || confirmUserIntent(msg, yesMode)){
                refreshPath(asmOutputPath, "Unable to refresh compiler output path at " + asmOutputPath);
            } else {
                System.out.println("Compilation aborted");
                System.exit(1);
            }

            //TODO respect return code
            Compiler.compile(ppuFiles, asmOutputPath, verbose, yesMode);
            System.out.println("Compilation successfully finished.");
        }

        //optionally assemble and link
        //ASSEMBLING
        if (compile && assemble) {
            //delete and recreate output directory
            msg = "Assembling will delete all files and directories in " + objOutputPath + ". Are you sure? (y/n)";
            if(cleanup || confirmUserIntent(msg, yesMode)){
                refreshPath(objOutputPath, "Unable to refresh assembler output path at " + objOutputPath);
            }
            else{
                System.out.println("Assembling aborted");
                System.exit(1);
            }

            //TODO respect return code
            Assembler.assemble(asmOutputPath, objOutputPath, verbose, yesMode);
            System.out.println("Assembling successfully finished.");
        }

        //LINKING
        if (compile && assemble && link) {
            //TODO respect return code
            Linker.link(objOutputPath, libraryPaths, linkerOutputName, verbose, yesMode);
            System.out.println("Linkage successfully finished.");
        }


        //CLEANUP
        if (cleanup) {
            deletePath(ppOutputPath, "Failed to delete preprocessor output");
            deletePath(asmOutputPath, "Failed to delete compiler output");
            deletePath(objOutputPath, "Failed to delete assembler output");
        }

        System.exit(0);
    }

    public static void showHelp() {
        //TODO help message
        System.out.println("Help message not written yet :/");
    }

    public static void refreshPath(Path path, String failMsg) {
        try {
            Runtime.getRuntime().exec(new String[] {"rm", "-r", path.toString()});
            Runtime.getRuntime().exec(new String[] {"mkdir", path.toString()});
        }
        catch (IOException e){
            System.out.println(failMsg);
            System.exit(-1);
        }
    }

    public static void deletePath(Path path, String failMsg) {
        try {
            Runtime.getRuntime().exec(new String[] {"rm", "-r", path.toString()});
        }
        catch (IOException e){
            System.out.println(failMsg);
            System.exit(-1);
        }
    }

    private static ArrayList<Path> detectFiles(Path path, String regex) throws IOException {
        if (!Files.exists(path)){
            System.out.println("Attempted to detect files in " + path + " but it does not exist");
            return new ArrayList<>();
        }

        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile)
                    .filter((s) -> s.toString().matches(regex))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    public static boolean confirmUserIntent(String message, boolean yesMode) {
        return confirmUserIntent(message, yesMode, false);
    }
    public static boolean confirmUserIntent(String message, boolean yesMode, boolean safeOption){
        if (yesMode){
            System.out.println("Yes mode: Ignoring prompt \"" + message + "\"");
            return true;
        }

        System.out.println(message);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            return reader.readLine().charAt(0) == 'y';
        }
        catch (IOException e){
            //unable to read from System.in. very weird - default to safe option
            System.out.println("Yes mode: Unable to confirm user intent. Defaulting to: " + safeOption);
            return safeOption;
        }
    }
}