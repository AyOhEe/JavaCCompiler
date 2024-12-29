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
    public static List<Path> preprocess(List<Path> sourceFiles, List<Path> includePaths, Path ctxPath, Path ppOutputPath, boolean yesMode, boolean verbose) throws CompilerException {
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


    private static Path preprocessFile(Path sf, List<Path> includePaths, PreprocessingContext context, Path ppOutputPath) throws CompilerException {
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
                for (; newlineDuplications > 0; --newlineDuplications) {
                    sb.append('\n');
                }
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
                return executeDirective(tokens, includePaths, i + 1, context);
            } else {
                throw new CompilerException("Invalid preprocessing directive: " + context.getCurrentSourcePath());
            }
        }

        //TODO macro replacement

        return i + 1;
    }

    private static int executeDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) throws CompilerException {
        PreprocessingToken token = tokens.get(i);
        tokens.remove(i); //directive name itself
        tokens.remove(i - 1); //hashtag
        //leaving the first token afterwards now at i - 1

        return switch (token.toString()) {
            case "if" -> ifDirective(tokens, includePaths, i - 1, context);
            case "ifdef" -> ifdefDirective(tokens, includePaths, i - 1, context);
            case "ifndef" -> ifndefDirective(tokens, includePaths, i - 1, context);
            case "elif" -> elifDirective(tokens, includePaths, i - 1, context);
            case "else" -> elseDirective(tokens, includePaths, i - 1, context);
            case "endif" -> endifDirective(tokens, includePaths, i - 1, context);
            case "include" -> includeDirective(tokens, includePaths, i - 1, context);
            case "define" -> defineDirective(tokens, includePaths, i - 1, context);
            case "undef" -> undefDirective(tokens, includePaths, i - 1, context);
            case "line" -> lineDirective(tokens, includePaths, i - 1, context);
            case "error" -> errorDirective(tokens, includePaths, i - 1, context);
            case "pragma" -> pragmaDirective(tokens, includePaths, i - 1, context);

            default -> invalidDirective(tokens, includePaths, i - 1, context, token);
        };
    }

    private static int ifDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) {
        //TODO this
        return i + 1;
    }

    private static int ifdefDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) throws CompilerException {
        PreprocessingToken token = tokens.remove(i);
        if (!token.is(PreprocessingToken.TokenType.IDENTIFIER)) {
            throw new CompilerException("#ifdef statement without valid identifier: " + context.getCurrentSourcePath());
        }

        List<PreprocessingToken> newIfDirective = new ArrayList<>();
        newIfDirective.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, "#"));
        newIfDirective.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "if"));
        newIfDirective.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "defined"));
        newIfDirective.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, "("));
        newIfDirective.add(token);
        newIfDirective.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, ")"));

        tokens.addAll(i, newIfDirective);

        return i;
    }

    private static int ifndefDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) throws CompilerException {
        PreprocessingToken token = tokens.remove(i);
        if (!token.is(PreprocessingToken.TokenType.IDENTIFIER)) {
            throw new CompilerException("#ifndef statement without valid identifier: " + context.getCurrentSourcePath());
        }

        List<PreprocessingToken> newIfDirective = new ArrayList<>();
        newIfDirective.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, "#"));
        newIfDirective.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "if"));
        newIfDirective.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, "!"));
        newIfDirective.add(new PreprocessingToken(PreprocessingToken.TokenType.IDENTIFIER, "defined"));
        newIfDirective.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, "("));
        newIfDirective.add(token);
        newIfDirective.add(new PreprocessingToken(PreprocessingToken.TokenType.OPERATOR_PUNCTUATOR, ")"));

        tokens.addAll(i, newIfDirective);

        return i;
    }

    private static int elifDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) throws CompilerException {
        throw new CompilerException("Unmatched #elif directive: " + context.getCurrentSourcePath());
    }

    private static int elseDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) throws CompilerException {
        throw new CompilerException("Unmatched #else directive: " + context.getCurrentSourcePath());
    }

    private static int endifDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) throws CompilerException {
        throw new CompilerException("Unmatched #endif directive: " + context.getCurrentSourcePath());
    }

    private static int includeDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) throws CompilerException {
        PreprocessingToken headerName = tokens.remove(i);
        if (!headerName.is(PreprocessingToken.TokenType.HEADER_NAME)) {
            throw new CompilerException("#include directive not followed by valid header name: " + context.getCurrentSourcePath());
        }

        boolean isQHeader = headerName.toString().startsWith("\"");
        String headerAsString = headerName.toString();
        String headerPath = headerAsString.substring(1, headerAsString.length() - 1);
        if (isQHeader) {
            return includeQHeader(tokens, includePaths, i, context, headerPath);
        } else {
            return includeHHeader(tokens, includePaths, i, context, headerPath);
        }
    }

    private static int includeQHeader(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context, String headerPath) throws CompilerException {
        //TODO this

        Path resolved = context.getCurrentSourcePath().resolve(headerPath);
        if (tryIncludeFile(resolved, tokens, i, includePaths, context)) {
            return i;
        }

        for (Path includePath : includePaths) {
            resolved = includePath.resolve(headerPath);
            if (tryIncludeFile(resolved, tokens, i, includePaths, context)) {
                return i;
            }
        }

        //TODO check built in. no built-in headers currently exist.

        throw new CompilerException("Attempted to include nonexistent file: " + headerPath + " in " + context.getCurrentSourcePath());
    }

    private static int includeHHeader(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context, String headerPath) throws CompilerException {
        //TODO check built in. no built-in headers currently exist.

        for (Path includePath : includePaths) {
            Path resolved = includePath.resolve(headerPath);
            if (tryIncludeFile(resolved, tokens, i, includePaths, context)) {
                return i;
            }
        }

        throw new CompilerException("Attempted to include nonexistent file: " + headerPath + " in " + context.getCurrentSourcePath());
    }

    private static boolean tryIncludeFile(Path resolved, List<PreprocessingToken> tokens, int i, List<Path> includePaths, PreprocessingContext context) throws CompilerException {
        if (!Files.exists(resolved)) {
            return false;
        }

        //TODO line directives
        String contents = readFileToString(resolved);
        tokens.addAll(i, preprocessString(resolved, contents, includePaths, context));
        return true;
    }


    private static int defineDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) throws CompilerException {
        PreprocessingToken label = tokens.get(i);
        if (label.is(PreprocessingToken.TokenType.FUNCTIONLIKE_MACRO_DEFINITION)) {
            return context.defineFunctionlike(tokens, i);
        } else if (label.is(PreprocessingToken.TokenType.IDENTIFIER)) {
            return context.defineObjectlike(tokens, i);
        } else {
            throw new CompilerException("Poorly formed #define directive in " + context.getCurrentSourcePath());
        }
    }

    private static int undefDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) throws CompilerException {
        if (!tokens.get(i).is(PreprocessingToken.TokenType.IDENTIFIER)) {
            throw new CompilerException("Poorly formed #undef directive in " + context.getCurrentSourcePath());
        }
        context.undefine(tokens.get(i).toString());
        tokens.remove(i);
        return i;
    }

    private static int lineDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) {
        //TODO this
        return i + 1;
    }

    private static int errorDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) throws CompilerException {
        PreprocessingToken message = tokens.get(i);
        if (message.is(PreprocessingToken.TokenType.STRING_LIT)) {
            throw new CompilerException("#error directive in " + context.getCurrentSourcePath() + ": " + message.unescapedString());
        }
        throw new CompilerException("Poorly formed #error directive in " + context.getCurrentSourcePath());
    }

    private static int pragmaDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context) {
        //pragma directives currently do nothing and are entirely ignored
        for (int j = i; j < tokens.size(); ++j) {
            if (tokens.get(j).is(PreprocessingToken.TokenType.NEWLINE)) {
                for (int k = 0; k < (j - i); ++k) {
                    tokens.remove(i);
                }
                break;
            }
        }

        return i + 1;
    }

    private static int invalidDirective(List<PreprocessingToken> tokens, List<Path> includePaths, int i, PreprocessingContext context, PreprocessingToken token) throws CompilerException {
        throw new CompilerException("Invalid preprocessing directive in: " + context.getCurrentSourcePath() + ": found directive " + token.toString());
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
