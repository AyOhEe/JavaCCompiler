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
    //TODO update
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

    //TODO update
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

    //TODO update
    private static void loadContext(Path sf, PreprocessingContext context) throws CompilerException {
        List<String> lines = openAsLines(sf);
        preprocessLines(sf, lines, new ArrayList<>(), context);
    }


    //TODO update
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

    //TODO update
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

    //TODO update
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

    //TODO update
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

    //TODO update
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

    //TODO update
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
