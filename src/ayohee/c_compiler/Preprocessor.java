package ayohee.c_compiler;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Preprocessor {
    public static ArrayList<Path> preprocess(ArrayList<Path> sourceFiles, ArrayList<Path> includePaths, Path ctxPath, Path ppOutputPath, boolean yesMode, boolean verbose) throws CompilerException {
        ArrayList<Path> compilationUnits = new ArrayList<>();
        LocalDateTime compilationTime = LocalDateTime.now();
        for (Path sf : sourceFiles) {
            System.out.println("\nPreprocessing " + sf.toString());
            PreprocessingContext context = findPPCtx(ctxPath, sf, compilationTime, yesMode, verbose); //refresh context between translation units

            Path result = preprocessFile(sf, includePaths, context, ppOutputPath);
            compilationUnits.add(result);
        }

        return compilationUnits;
    }

    private static PreprocessingContext findPPCtx(Path ctxPath, Path sf, LocalDateTime compilationTime, boolean yesMode, boolean verbose) throws CompilerException {
        PreprocessingContext ctx = new PreprocessingContext(sf, compilationTime, yesMode, verbose);
        if (Files.exists(ctxPath)) {
            if (verbose) {
                System.out.println("Context file found. Loading constants via preprocessor...");
            }
            loadContext(ctxPath, ctx);
        } else if (verbose) {
            System.out.println("Context file not found or not supplied. Using blank context.");
        }
        return ctx;
    }

    private static void loadContext(Path sf, PreprocessingContext context) throws CompilerException {
        String fileContents = readFileToString(sf);
        preprocessString(sf, fileContents, new ArrayList<>(), context);
    }


    private static Path preprocessFile(Path sf, ArrayList<Path> includePaths, PreprocessingContext context, Path ppOutputPath) throws CompilerException {
        String fileContents = readFileToString(sf);
        List<PreprocessingToken> tokens = preprocessString(sf, fileContents, includePaths, context);

        Path compilationUnitPath = Paths.get(ppOutputPath.toAbsolutePath().toString(), getUnitFilename(context.getOriginalSourcePath()));
        if (tokens.isEmpty()) {
            return compilationUnitPath;
        }

        try (FileWriter writer = new FileWriter(compilationUnitPath.toFile())) {
            for (PreprocessingToken token : tokens) {
                writer.write(token.toString());
                if (token.getType() != PreprocessingToken.TokenType.NEWLINE) {
                    writer.write(" ");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to write " + compilationUnitPath);
            System.exit(2);
        }

        return compilationUnitPath;
    }

    private static String readFileToString(Path sf) {
        try {
            return Files.readString(sf);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to read " + sf);
            System.exit(2);
            return "";
        }
    }

    private static String getUnitFilename(Path sf) {
        //trim .c
        String filename = sf.getFileName().toString();
        return filename.substring(0, filename.length() - 2) + ".i";
    }

    private static List<PreprocessingToken> preprocessString(Path filePath, String fileContents, List<Path> includePaths, PreprocessingContext context) throws CompilerException {
        if(fileContents.isBlank()){
            return new ArrayList<>();
        }
        context.fileDeeper(filePath);

        //phase 1: trigraph replacement
        String workingContents = replaceTrigraphs(fileContents);

        //phase 2: eof == newline enforcement and \ + \n removal
        workingContents = ensureEOFNewline(workingContents, context);
        workingContents = mergeSourceLines(workingContents);

        //phase 3: tokenization and comment removal
        List<PreprocessingToken> tokens = Tokenizer.tokenize(workingContents, context);

        //phase 4: preprocessing directive execution and macro expansion. #include + 1-4 happens here
        tokens = executeDirectives(tokens, includePaths, context);

        //phase 5 and 6 technically count as preprocessor responsibilities,
        //but practically belong to the compiler and should be handled after tokenisation

        context.fileOut();
        return tokens;
    }


    private static String replaceTrigraphs(String fileContents) {
        return fileContents
                .replace("??=", "#")
                .replace("??(", "[")
                .replace("??/", "\\")
                .replace("??)", "]")
                .replace("??'", "^")
                .replace("??<", "{")
                .replace("??!", "|")
                .replace("??>", "}")
                .replace("??-", "~")
                .replace("\r\n", "\n"); //i hate carriage returns. make newline detection and manipulation awful
    }


    private static String mergeSourceLines(String fileContents) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int newlineDuplications = 0;
        while (i < fileContents.length()) {
            if (fileContents.charAt(i) == '\n' && newlineDuplications != 0) {
                for (int j = 0; j < newlineDuplications; ++j) {
                    sb.append('\n');
                }
                newlineDuplications = 0;
            }

            if (fileContents.charAt(i) == '\\' && fileContents.charAt(i + 1) == '\n') {
                i += 2;
                newlineDuplications += 1;
                continue;
            }

            sb.append(fileContents.charAt(i));
            ++i;
        }

        //any newlines that haven't been introduced at the end of the file should be placed now
        for (int j = 0; j < newlineDuplications; ++j) {
            sb.append('\n');
        }

        return sb.toString();
    }

    private static String ensureEOFNewline(String fileContents, PreprocessingContext context) throws CompilerException {
        if (fileContents.endsWith("\\\n")) {
            throw new CompilerException("Backslash-newline at end of file: " + context.getCurrentSourcePath());
        }
        if (fileContents.isBlank() || fileContents.endsWith("\n")) {
            return fileContents;
        }

        if (fileContents.endsWith("\\")) {
            throw new CompilerException("Backslash-newline at end of file: " + context.getCurrentSourcePath());
        } else {
            return fileContents + '\n';
        }
    }

    private static List<PreprocessingToken> executeDirectives(List<PreprocessingToken> tokens, List<Path> includePaths, PreprocessingContext context) throws CompilerException {
        for (int i = 0; i < tokens.size();) {
            i = handleToken(tokens, includePaths, i, context);
        }

        return tokens;
    }

    private static int handleToken(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) throws CompilerException {
        PreprocessingToken currentToken = tokens.get(i);
        if (currentToken.is("#") && (i == 0 || tokens.get(i - 1).is(PreprocessingToken.TokenType.NEWLINE))) {
            if (i + 1 < tokens.size() && tokens.get(i + 1).is(PreprocessingToken.TokenType.IDENTIFIER)) {
                return executeDirective(tokens, includePaths, i + 1);
            } else {
                throw new CompilerException("Invalid preprocessing directive: " + context.getCurrentSourcePath());
            }
        }

        //TODO macro replacement

        return i + 1;
    }

    private static int executeDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i) {
        System.out.println("Found directive: " + tokens.get(i).toString());
        return i;
    }

    //TODO verify this is correct
    public static boolean isValidIdentifier(String identifier) throws CompilerException {
        switch (identifier) {
            case "__LINE__", "__FILE__", "__DATE__", "__TIME__", "__STDC__" -> throw new CompilerException("Attempted to redefine or undefine predefined macro: " + identifier);
            case "" -> throw new CompilerException("Empty identifier");
        }

        if (!(Character.isAlphabetic(identifier.charAt(0)) || identifier.charAt(0) == '_')) {
            return false;
        }

        for (int i = 1; i < identifier.length(); ++i) {
            if (!(Character.isLetterOrDigit(identifier.charAt(i)) || identifier.charAt(i) == '_')) {
                return false;
            }
        }

        return true;
    }
}
