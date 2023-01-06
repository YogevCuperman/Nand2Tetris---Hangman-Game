import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Parser {

    BufferedReader reader;
    public String currentLine;


    public Parser(File file) throws IOException{
        // opening the file we need to read
        this.reader = new BufferedReader(new FileReader(file));
        this.currentLine = "";
    }

    public enum Commands {
        ARITHMETIC,
        PUSH,
        POP,
        LABEL,
        GOTO,
        IF,
        FUNCTION,
        RETURN,
        CALL
    }

    public boolean advance() throws IOException {
        currentLine = reader.readLine();
        if(currentLine == null) return false;       // checking if there is more lines to read
        if(currentLine.startsWith("//")) return advance();  // ignoring whole-line comments
        if(currentLine.indexOf("//") > 0) currentLine = currentLine.split("//")[0];   // ignoring in-line comments
        currentLine = currentLine.trim();           // ignoring whitespaces
        if(currentLine.isEmpty()) return advance(); // if it's an empty line - read the next line
        return true;        // true means that we successfully read a line
    }

    public Commands commandType() {
        if(currentLine.contains("push")) return Commands.PUSH;
        else if (currentLine.contains("pop")) return Commands.POP;
        else if (currentLine.contains("call")) return Commands.CALL;
        else if (currentLine.contains("function")) return Commands.FUNCTION;
        else if (currentLine.contains("return")) return Commands.RETURN;
        else if (currentLine.contains("label")) return Commands.LABEL;
        else if (currentLine.contains("if-goto")) return Commands.IF;
        else if (currentLine.contains("goto")) return Commands.GOTO;
        else return Commands.ARITHMETIC;
    }

    public String arg1() {
        // should not be called for RETURN command

        if(commandType() == Commands.ARITHMETIC) return currentLine;
        else {
            String arg1 = currentLine.split(" ")[1];
            return arg1;
        }
    }

    public int arg2() {
        // should be only called if the command is push/pop/function/call

        String arg2 = currentLine.split(" ")[2];
        return Integer.parseInt(arg2);
    }


}
