package ayohee.c_compiler;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Preprocessor {
    public static ArrayList<Path> preprocess(ArrayList<Path> sourceFiles, ArrayList<Path> includePaths, Path ppOutputPath, boolean yesMode, boolean verbose) {
        ArrayList<Path> compilationUnits = new ArrayList<>();
        for (Path sf : sourceFiles) {
            Path result = preprocessFile(sf, includePaths, ppOutputPath, verbose);
            compilationUnits.add(result);
        }

        return compilationUnits;
    }

    private static Path preprocessFile(Path sf, ArrayList<Path> includePaths, Path ppOutputPath, boolean verbose) {
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

    private static List<String> preprocessLines(List<String> lines, ArrayList<Path> includePaths, boolean verbose) {
        if(lines.isEmpty()){
            return lines;
        }

        List<String> modifiedLines = new ArrayList<String>(lines);

        replaceTrigraphs(modifiedLines, verbose); //phase 1: trigraph replacement
        replaceLineMacro(modifiedLines, verbose); //in between phase: replace __LINE__ macros before source line merging
        mergeSourceLines(modifiedLines, verbose); //phase 2: eof == newline enforcement and \ + \n removal
        ensureEOFNewline(modifiedLines, verbose);
        //phase 3: comment removal
        //phase 4: preprocessing directive execution and macro expansion. #include + 1-4 happens here

        //phase 5 and 6 technically count as preprocessor responsibilities,
        //but practically belong to the compiler and should be handled after tokenisation

        return modifiedLines;
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
