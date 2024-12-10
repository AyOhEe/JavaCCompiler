package ayohee.c_compiler;

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
        List<String> lines = openAsLines(sf);
        preprocessLines(sf, lines, new ArrayList<>(), context);
    }


    private static Path preprocessFile(Path sf, ArrayList<Path> includePaths, PreprocessingContext context, Path ppOutputPath) throws CompilerException {
        List<String> lines = openAsLines(sf);
        List<String> processedLines = preprocessLines(sf, lines, includePaths, context);


        Path compilationUnitPath = Paths.get(ppOutputPath.toAbsolutePath().toString(), getUnitFilename(context.getOriginalSourcePath()));
        try (FileWriter writer = new FileWriter(compilationUnitPath.toFile())) {
            for (String processedLine : processedLines) {
                writer.write(processedLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to write " + compilationUnitPath);
            System.exit(2);
        }

        return compilationUnitPath;
    }

    private static List<String> openAsLines(Path sf) {
        List<String> lines = null;
        try {
            lines = Files.readAllLines(sf);
            for(int i = 1; i < lines.size(); ++i) {
                lines.set(i - 1, lines.get(i - 1) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to open " + sf);
            System.exit(2);
        }
        return lines;
    }

    private static String getUnitFilename(Path sf) {
        //trim .c
        String filename = sf.getFileName().toString();
        return filename.substring(0, filename.length() - 2) + ".i";
    }

    private static List<String> preprocessLines(Path filePath, List<String> lines, List<Path> includePaths, PreprocessingContext context) throws CompilerException {
        if(lines.isEmpty()){
            return lines;
        }
        context.fileDeeper(filePath);

        List<String> modifiedLines = new ArrayList<>(lines);

        //phase 1: trigraph replacement
        replaceTrigraphs(modifiedLines);

        //phase 2: eof == newline enforcement and \ + \n removal
        mergeSourceLines(modifiedLines);
        ensureEOFNewline(modifiedLines);

        //phase 3: comment removal
        removeComments(modifiedLines, context.isVerbose());
        //phase 4: preprocessing directive execution and macro expansion. #include + 1-4 happens here
        executeDirectives(modifiedLines, includePaths, context);

        //phase 5 and 6 technically count as preprocessor responsibilities,
        //but practically belong to the compiler and should be handled after tokenisation

        context.fileOut();
        return modifiedLines;
    }

    private static void executeDirectives(List<String> lines, List<Path> includePaths, PreprocessingContext context) throws CompilerException {
        for (int i = 0; i < lines.size(); /*deliberately empty - directives move lines themselves*/) {
            String trimmed = lines.get(i).stripLeading();
            if (trimmed.isEmpty()) {
                ++i;
                continue;
            }

            String directive = extractDirective(trimmed);
            if(!directive.startsWith("#")) {
                context.doReplacement(lines, i);
                ++i;
                continue;
            }

            //#line directives are ignored - those are for the compiler
            i = switch (directive) {
                case "#if" -> ifDirective(trimmed, i, lines, context);
                case "#ifdef" -> ifdefDirective(trimmed, i, lines);
                case "#ifndef" -> ifndefDirective(trimmed, i, lines);
                case "#elif" -> elifDirective();
                case "#else" -> elseDirective();
                case "#endif" -> endifDirective();

                case "#include" -> includeDirective(trimmed, i, lines, includePaths, context);
                case "#define" -> defineDirective(trimmed, i, lines, context);
                case "#undef" -> undefineDirective(trimmed, i, lines, context);
                case "#error" -> errorDirective(trimmed, context);
                case "#pragma" -> pragmaDirective(i, lines);
              //case "": # empty statement - should be ignored

                default -> i + 1;
            };
        }
    }

    private static int ifDirective(String trimmed, int i, List<String> lines, PreprocessingContext context) throws CompilerException {
        //find else/elif/endif
        int depth = 0;
        int j = i; //declared outside to keep the value after the loop finishes
        int currentClauseEnding = -1;
        int blockEnding = -1;
        for (; j < lines.size(); ++j) {
            String jLine = lines.get(j).stripLeading();
            String directive = extractDirective(jLine);
            if (directive.contentEquals("#if") || directive.contentEquals("#ifdef") || directive.contentEquals("#ifndef")) {
                depth += 1;
            } else if (directive.contentEquals("#endif")) {
                depth -= 1;
            }

            if (depth == 1 && (directive.contentEquals("#else") || directive.contentEquals("#elif"))) {
                if (currentClauseEnding == -1) {
                    currentClauseEnding = j;
                }
            }

            if (depth == 0) {
                if (currentClauseEnding == -1) {
                    currentClauseEnding = j;
                }
                blockEnding = j;
                break;
            }
        }
        if (blockEnding == -1) {
            throw new CompilerException("#if with no corresponding #endif");
        }


        //regex matches one or more continuous whitespace characters (except newlines) via a double negative
        //TL;DR, it replaces each group of whitespace with a single space
        String evaluatedLine = context.doReplacement(trimmed).replaceAll("[^\\S\\n]+", " ");
        if (evaluatedLine.startsWith("#if 1")) {
            //keep the clause, get rid of the rest of the block
            lines.set(i, "\n");
            for (int k = currentClauseEnding; k < blockEnding + 1; ++k) {
                lines.set(k, "\n");
            }

            return i + 1;
        }
        else if(evaluatedLine.startsWith("#if 0")) {
            //get rid of the clause, modify the #else/#elif to an #if 1/#elif {condition}
            for (int k = i; k < currentClauseEnding; ++k) {
                lines.set(k, "\n");
            }

            String line = lines.get(currentClauseEnding).stripLeading();
            String directive = extractDirective(line);

            if (directive.contentEquals("#elif")) {
                lines.set(currentClauseEnding, "#if" + line.substring(5));
            } else if (directive.contentEquals("#else")) {
                lines.set(currentClauseEnding, "#if 1\n");
            } else if (directive.contentEquals("#endif")) {
                lines.set(currentClauseEnding, "\n");
            }
            return currentClauseEnding;
        } else {
            throw new CompilerException("#if directive had non-evaluatable condition: " + trimmed);
        }
    }

    private static int ifdefDirective(String trimmed, int i, List<String> lines) {
        lines.set(i, "#if defined(" + trimmed.substring(7, trimmed.length() - 1) + ")\n");
        return i;
    }

    private static int ifndefDirective(String trimmed, int i, List<String> lines) {
        lines.set(i, "#if !defined(" + trimmed.substring(8, trimmed.length() - 1) + ")\n");
        return i;
    }

    private static int elifDirective() throws CompilerException {
        throw new CompilerException("Unmatched #elif directive");
    }

    private static int elseDirective() throws CompilerException {
        throw new CompilerException("Unmatched #else directive");
    }

    private static int endifDirective() throws CompilerException {
        throw new CompilerException("Unmatched #endif directive");
    }

    private static int includeDirective(String trimmed, int i, List<String> lines, List<Path> includePaths, PreprocessingContext context) throws CompilerException {
        int j = 8; //skip "#include"
        for(; j < trimmed.length(); ++j) {
            if (!Character.isWhitespace(trimmed.charAt(j))) {
                break;
            }
        }

        Path includePath = null;
        if (trimmed.charAt(j) == '<') {
            //angle include: check built-ins, then supplied
            String path = extractIncludePath(trimmed, j, '>');
            includePath = findAngleInclude(path, j, includePaths, context);
        } else if (trimmed.charAt(j) == '"') {
            //quote include: check local first, then supplied, then try for built-ins
            String path = extractIncludePath(trimmed, j, '"');
            includePath = findQuoteInclude(path, j, includePaths, context);
        } else {
            throw new CompilerException("Incorrectly formed #include: " + trimmed);
        }

        //preprocess the new file before inclusion
        List<String> includedLines = openAsLines(includePath);
        includedLines = preprocessLines(includePath, includedLines, includePaths, context);

        //TODO line directives
        //remove the include
        lines.set(i, "\n");
        //insert the lines
        for (int k = includedLines.size() - 1; k >= 0; --k) {
            lines.add(i + 1, includedLines.get(k));
        }

        return i + 1;
    }

    private static Path findAngleInclude(String path, int j, List<Path> includePaths, PreprocessingContext context) throws CompilerException {
        Path asPath = Path.of(path);

        //TODO check builtin headers
        for (Path includePath : includePaths) {
            Path fullPath = asPath.isAbsolute() ? asPath : includePath.resolve(asPath);
            System.out.println(fullPath);
            if (Files.exists(fullPath)) {
                return fullPath;
            }
        }
        throw new CompilerException("Failed to locate included file: " + path);
    }

    private static Path findQuoteInclude(String path, int j, List<Path> includePaths, PreprocessingContext context) throws CompilerException {
        Path asPath = Path.of(path);

        Path localPath = asPath.isAbsolute() ? asPath : context.getCurrentSourcePath().getParent().resolve(asPath);
        if (Files.exists(localPath)) {
            return localPath;
        }

        for (Path includePath : includePaths) {
            Path fullPath = asPath.isAbsolute() ? asPath : includePath.resolve(asPath);
            if (Files.exists(fullPath)) {
                return fullPath;
            }
        }
        //TODO check builtin headers
        throw new CompilerException("Failed to locate included file: " + path);
    }

    private static String extractIncludePath(String trimmed, int j, char delimiter) throws CompilerException {
        int backslashCount = 0;
        for (int k = j + 1; k < trimmed.length(); ++k) {
            if (trimmed.charAt(k) == '\\') {
                backslashCount += 1;
            } else {
                backslashCount = 0;
            }

            if (trimmed.charAt(k) == delimiter && backslashCount % 2 == 0) {
                return trimmed.substring(j + 1, k);
            }
        }
        throw new CompilerException("#include did not close: " + trimmed);
    }


    private static int defineDirective(String trimmed, int i, List<String> lines, PreprocessingContext context) throws CompilerException {
        context.define(trimmed);
        lines.set(i, "\n");
        return i + 1;
    }

    private static int undefineDirective(String trimmed, int i, List<String> lines, PreprocessingContext context) throws CompilerException {
        String identifier = PreprocessorDefinition.findIdentifier(trimmed, 7);
        if (isValidIdentifier(identifier)) {
            context.undefine(identifier);
        } else {
            throw new CompilerException("Attempted to undefine invalid identifier \"" + identifier + "\"" + " on line: " + trimmed);
        }

        lines.set(i, "\n");
        return i + 1;
    }

    private static int errorDirective(String trimmed, PreprocessingContext context) throws CompilerException {
        throw new CompilerException(context.doReplacement(trimmed));
    }

    private static int pragmaDirective(int i, List<String> lines) {
        //currently, no pragma directives do anything.
        lines.set(i, "\n");
        return i + 1;
    }


    private static String extractDirective(String trimmed) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trimmed.length(); ++i) {
            if(Character.isWhitespace(trimmed.charAt(i))) {
                break;
            }
            sb.append(trimmed.charAt(i));
        }

        return sb.toString();
    }


    private static void removeComments(List<String> lines, boolean verbose) throws CompilerException {
        boolean inMultiline = false;
        boolean wasInMultiline = false;
        boolean inString = false;
        boolean inChar = false;
        for(int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i);
            char lastChar = '-'; //doesn't matter what it is as long as it's not something we check for
            int backslashCount = 0;

            int multilineBegin = -1;

            for(int j = 0; j < line.length(); ++j) {
                char atJ = line.charAt(j);
                if (atJ == '\\') {
                    backslashCount++;
                    lastChar = atJ;
                    continue;
                }

                if (atJ == '"' && !inMultiline && !inChar) {
                    //only exit string literals if the quote is escaped
                    inString = !(inString && backslashCount % 2 == 0);
                }
                else if (atJ == '\'' && !inMultiline && !inString) {
                    //only exit char literals if the quote is escaped
                    inChar = !(inChar && backslashCount % 2 == 0);
                }
                else if (lastChar == '/' && atJ == '/' && !(inString || inChar) && !inMultiline) {
                    //double-slash comment. remove the slashes and the rest of the line.
                    //newline gets removed in the substring, so replace it
                    line = line.substring(0, j - 1) + "\n";
                    lines.set(i, line);
                    break;
                }
                else if (lastChar == '/' && atJ == '*' && !(inString || inChar) && !inMultiline) {
                    //beginning of a multiline comment. don't remove text until we know the extent
                    inMultiline = true;
                    multilineBegin = j - 1;
                }
                else if (lastChar == '*' && atJ == '/' && !(inString || inChar) && inMultiline && multilineBegin != j - 2) {
                    //end of a multiline comment
                    inMultiline = false;

                    if (multilineBegin != -1) {
                        //only covered this line. remove the section and continue
                        line = line.substring(0, multilineBegin) + line.substring(j + 1);
                        lines.set(i, line);

                        j = multilineBegin - 1; //continue from where the comment began (subtract 1 to account for ++j)
                        lastChar = j != -1 ? line.charAt(j) : '-';

                        multilineBegin = -1; //reset to ensure later comments on this line work fine
                        continue;
                    } else {
                        //covered multiple lines. remove everything before this
                        line = line.substring(j + 1);
                        lines.set(i, line);

                        j = -1; //continue from index 0 (subtract 1 to account for ++j)
                        lastChar = '-';
                        continue;
                    }
                }

                lastChar = atJ;
                backslashCount = 0;
            }

            if(multilineBegin != -1) {
                lines.set(i, line.substring(0, multilineBegin) + "\n");
            }

            //last line was a comment and this line is entirely a comment. remove it
            if (wasInMultiline && inMultiline) {
                lines.set(i, "\n");
            }

            wasInMultiline = inMultiline;
        }
    }

    private static void ensureEOFNewline(List<String> lines) {
        String lastLine = lines.getLast();
        if (lastLine.isEmpty()) {
            lines.set(lines.size() - 1, "\n");
        } else {
            if (!lastLine.endsWith("\n")) {
                lines.set(lines.size() - 1, lastLine + "\n");
            }
        }

        if (lastLine.contains("\\\n")) {
            //TODO warning
            System.out.println("Backslash-newline at end of file");
        }
    }

    private static void mergeSourceLines(List<String> lines) {
        for (int i = lines.size() - 2; i > -1; --i) {
            String line = lines.get(i);
            if (line.length() < 2) {
                continue;
            }

            if (line.endsWith("\\\n")) {
                lines.set(i, line.substring(0, line.length() - 2) + lines.get(i + 1));
                lines.set(i + 1, "\n");
            }
        }
    }

    private static void replaceTrigraphs(List<String> lines) {
        lines.replaceAll(s -> s
                .replace("??=", "#")
                .replace("??(", "[")
                .replace("??/", "\\")
                .replace("??)", "]")
                .replace("??'", "^")
                .replace("??<", "{")
                .replace("??!", "|")
                .replace("??>", "}")
                .replace("??-", "~"));
    }

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
