package ayohee.c_compiler;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Preprocessor {
    public static ArrayList<Path> preprocess(ArrayList<Path> sourceFiles, ArrayList<Path> includePaths, Path ppOutputPath, boolean yesMode, boolean verbose) throws CompilerException {
        ArrayList<Path> compilationUnits = new ArrayList<>();
        for (Path sf : sourceFiles) {
            Path result = preprocessFile(sf, includePaths, ppOutputPath, verbose);
            compilationUnits.add(result);
        }

        return compilationUnits;
    }

    private static Path preprocessFile(Path sf, ArrayList<Path> includePaths, Path ppOutputPath, boolean verbose) throws CompilerException {
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


        List<String> processedLines = preprocessLines(lines, includePaths, verbose);


        Path compilationUnitPath = Paths.get(ppOutputPath.toAbsolutePath().toString(), getUnitFilename(sf));
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

    private static String getUnitFilename(Path sf) {
        //trim .c
        String filename = sf.getFileName().toString();
        return filename.substring(0, filename.length() - 2) + ".i";
    }

    private static List<String> preprocessLines(List<String> lines, ArrayList<Path> includePaths, boolean verbose) throws CompilerException {
        if(lines.isEmpty()){
            return lines;
        }

        List<String> modifiedLines = new ArrayList<String>(lines);

        replaceTrigraphs(modifiedLines, verbose); //phase 1: trigraph replacement
        replaceLineMacro(modifiedLines, verbose); //in between phase: replace __LINE__ macros before source line merging
        mergeSourceLines(modifiedLines, verbose); //phase 2: eof == newline enforcement and \ + \n removal
        ensureEOFNewline(modifiedLines, verbose);
        removeComments(modifiedLines, verbose);   //phase 3: comment removal
        //phase 4: preprocessing directive execution and macro expansion. #include + 1-4 happens here

        //phase 5 and 6 technically count as preprocessor responsibilities,
        //but practically belong to the compiler and should be handled after tokenisation

        return modifiedLines;
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
                lines.set(i, line.substring(0, multilineBegin));
            }

            //last line was a comment and this line is entirely a comment. remove it
            if (wasInMultiline && inMultiline) {
                lines.set(i, "\n");
            }

            wasInMultiline = inMultiline;
        }
    }

    private static void ensureEOFNewline(List<String> lines, boolean verbose) {
        String lastLine = lines.getLast();
        if (lastLine.isEmpty()) {
            lines.set(lines.size() - 1, "\n");
        } else {
            if (lastLine.charAt(lastLine.length() - 1) != '\n') {
                lines.set(lines.size() - 1, lastLine + "\n");
            }
        }

        if (lastLine.contains("\\\n")) {
            //TODO warning
            System.out.println("Backslash-newline at end of file");
        }
    }

    private static void mergeSourceLines(List<String> lines, boolean verbose) {
        lines.replaceAll(s -> s.replace("\\\n", ""));
    }

    private static void replaceLineMacro(List<String> lines, boolean verbose) {
        for (int i = 0; i < lines.size(); ++i) {
            lines.set(i, lines.get(i).replaceAll("\\b__LINE__\\b", Integer.toString(i + 1)));
        }
    }

    private static void replaceTrigraphs(List<String> lines, boolean verbose) {
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

}
