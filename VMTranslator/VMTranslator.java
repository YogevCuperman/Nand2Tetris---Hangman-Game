import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VMTranslator {

    public static void main(String[] args) throws IOException {
//        String path = args[0];
        String path = "C:\\Users\\yogev\\Desktop\\Nand2Tetris\\projects\\08\\FunctionCalls\\FibonacciElement";
        List<File> files = new ArrayList<>();
        String outputFile;
        File f = new File(path);
        if(f.isDirectory()) {
           outputFile = path + "/" + f.getName() + ".asm";
           File[] dirFiles = f.listFiles();
           if(dirFiles == null) return;
           for(File file : dirFiles) {
               if(file.getName().endsWith(".vm")) {
                   files.add(file);
               }
           }
        } else {    // path is a file
            outputFile = path.replace(".vm", ".asm");
            files.add(f);
        }

        CodeWriter coder = new CodeWriter(new File(outputFile));

        if(f.isDirectory()) coder.writeInit();

        for(File currentFile : files) {
            String fileName = currentFile.getName();
//            fileName = fileName.substring(0, fileName.indexOf(".vm"));
            fileName = fileName.split(".vm")[0];
            coder.setFileName(fileName);
            Parser parser = new Parser(currentFile);
            while (parser.advance()) {
                    // checking which command should we translate:

                if(parser.commandType() == Parser.Commands.ARITHMETIC) {
                    coder.writeArithmetic(parser.arg1());
                } else if(parser.commandType() == Parser.Commands.PUSH) {
                    coder.writePush(parser.arg1(), parser.arg2());
                } else if(parser.commandType() == Parser.Commands.POP) {
                    coder.writePop(parser.arg1(), parser.arg2());
                } else if(parser.commandType() == Parser.Commands.LABEL) {
                    coder.writeLabel(parser.arg1());
                } else if(parser.commandType() == Parser.Commands.GOTO) {
                    coder.writeGoto(parser.arg1());
                } else if(parser.commandType() == Parser.Commands.IF) {
                    coder.writeIf(parser.arg1());
                } else if(parser.commandType() == Parser.Commands.FUNCTION) {
                    coder.writeFunction(parser.arg1(), parser.arg2());
                } else if(parser.commandType() == Parser.Commands.CALL) {
                    coder.writeCall(parser.arg1(), parser.arg2());
                } else if(parser.commandType() == Parser.Commands.RETURN) {
                    coder.writeReturn();
                }
            }
        }

        coder.close();
    }

    private static void getFiles(File input, ArrayList<File> files) throws FileNotFoundException {
        if(input.isFile()) {
            // check for .vm extension before adding to list of files
            String filename = input.getName();
            int extension = filename.indexOf('.');
            if(extension > 0) {
                String fileExtension = filename.substring(extension + 1);
                if(fileExtension.equalsIgnoreCase("vm")) {
                    files.add(input);
                }
            }
        } else if(input.isDirectory()) {
            File[] innerFiles = input.listFiles();
            for(File f : innerFiles) {
                getFiles(f, files);
            }
        } else {
            throw new FileNotFoundException("Could not find file or directory.");
        }
    }
}
