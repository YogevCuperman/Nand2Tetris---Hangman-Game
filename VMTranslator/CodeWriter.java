import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CodeWriter {

    private BufferedWriter writer;
    private String fileName;
    private int lineCounter;     // public counter of which line was written last
    private int labelCounter;     // for making return labels unique

    public CodeWriter(File writeFile) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(writeFile, false));
        this.fileName = "";
        this.lineCounter = -1;
        this.labelCounter = 0;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void close() throws IOException {
        writer.close();
    }

    public void writePush(String segment, int index) throws IOException {
        if (segment.equals("constant")) {
            writeLine("@" + index); //A=i
            writeLine("D=A");       //D=A (=i)
            writeLine("@SP");       //A=SP
            writeLine("A=M");       //A=RAM[SP]
            writeLine("M=D");       //RAM[SP]=D
            IncSP();                //SP++
        } else if (segment.equals("static")) {
            writeLine("@" + fileName + "." + index);
            writeLine("D=M");
            IncSP();
            writeLine("@SP");
            writeLine("A=M-1");
            writeLine("M=D");
        } else if(segment.equals("pointer")) {
            if(index == 0) writeLine("@THIS");
            else if(index == 1) writeLine("@THAT");
            writeLine("D=M");
            IncSP();
            writeLine("@SP");
            writeLine("A=M-1");
            writeLine("M=D");
        }else {    // segment != constant or static
            writeLine("@" + getSegmentKey(segment));      //A=segmentKey
            if(segment.equals("temp")) writeLine("D=A");    //D=5
            else writeLine("D=M");       //D=RAM[SEG]
            writeLine("@" + index); //A=i
            writeLine("A=D+A");     //A=SEG+i
            writeLine("D=M");       //D=RAM[addr]
            writeLine("@SP");       //A=SP
            writeLine("A=M");       //A=RAM[SP]
            writeLine("M=D");       //RAM[RAM[SP]] = RAM[addr]
            IncSP();                //SP++
        }
    }
    public void writePop(String segment, int index) throws IOException {
        if(segment.equals("static")) {
            DecSP();
            writeLine("@SP");
            writeLine("A=M");
            writeLine("D=M");
            writeLine("@" + fileName + "." + index);
            writeLine("M=D");
        } else if(segment.equals("pointer")) {
            DecSP();
            writeLine("@SP");
            writeLine("A=M");
            writeLine("D=M");
            if(index == 0) writeLine("@THIS");
            else if(index == 1) writeLine("@THAT");
            writeLine("M=D");
        } else {
            writeLine("@" + getSegmentKey(segment));      //A=segmentKey
            if(segment.equals("temp")) writeLine("D=A");    //D=5
            else writeLine("D=M");       //D=RAM[SEG]
            writeLine("@" + index); //A=i
            writeLine("D=D+A");     //D=SEG+i (addr)
            writeLine("@R13");
            writeLine("M=D");       //R13 stores addr
            DecSP();                //SP--
            writeLine("@SP");
            writeLine("A=M");
            writeLine("D=M");       //D=RAM[SP]
            writeLine("@R13");
            writeLine("A=M");       //A=addr
            writeLine("M=D");       //RAM[addr] = RAM[SP]
        }
    }

    public void writeArithmetic(String command) throws IOException {
        if(command.equals("add") || command.equals("sub") || command.equals("and") || command.equals("or")) {
            writeLine("@SP");
            writeLine("AM=M-1");
            writeLine("D=M");   // D = y (first pop)
            writeLine("A=A-1"); // A = address of x (second pop)
            if(command.equals("add")) writeLine("M=M+D");
            else if (command.equals("sub")) writeLine("M=M-D");
            else if (command.equals("and")) writeLine("M=M&D");
            else writeLine("M=M|D");
        } else if(command.equals("neg")) {
            writeLine("@SP");
            writeLine("A=M-1");
            writeLine("M=-M");  // RAM[RAM[SP] - 1] *= -1
        } else if(command.equals("not")) {
            writeLine("@SP");
            writeLine("A=M-1");
            writeLine("M=!M");  // ! RAM[RAM[SP] - 1]
        } else {
            // command is eq / lt / gt
            writeLine("@SP");
            writeLine("AM=M-1");
            writeLine("D=M");   // D = y (first pop)
            writeLine("A=A-1"); // A = address of x (second pop)
            writeLine("D=M-D"); // D = x - y
            writeLine("@" + (lineCounter + 8));
            if(command.equals("eq")) writeLine("D;JEQ");
            else if(command.equals("lt")) writeLine("D;JLT");
            else if(command.equals("gt")) writeLine("D;JGT");
            // if didn't jump:
            writeLine("@SP");
            writeLine("A=M-1");
            writeLine("M=0");       // writing false
            writeLine("@" + (lineCounter + 6));         // jump to what comes after
            writeLine("0;JMP");

            //if did jump:
            writeLine("@SP");
            writeLine("A=M-1");
            writeLine("M=-1");      // writing true
        }
    }

    public void writeLabel(String label) throws IOException {
        writeLine("(" + label + ")");
    }

    public void writeGoto(String label) throws IOException {
        writeLine("@" + label);
        writeLine("0;JMP");     // unconditional jump
    }

    public void writeIf(String label) throws IOException {
        writeLine("@SP");
        writeLine("AM=M-1");
        writeLine("D=M");   // D=pop()
        writeLine("@" + label);
        writeLine("D;JNE"); // conditional jump (true = -1, false = 0)
    }

    public void writeFunction(String functionName, int nVars) throws IOException {
        writeLabel(functionName);
        for(int i = 0; i < nVars; i++) {        // pushing constant 0 - nVars times
            writePush("constant", 0);
        }
    }

    public void writeCall(String functionName, int nArgs) throws IOException {
        String label = functionName + "$ret." + labelCounter;
        if(functionName.equals("Sys.init")) label = functionName;

        // pushing returnAddress
        writeLine("@" + label);
        writeLine("D=A");
        pushD();

        // push LCL
        writeLine("@LCL");
        writeLine("D=M");
        pushD();

        // push ARG
        writeLine("@ARG");
        writeLine("D=M");
        pushD();

        // push THIS
        writeLine("@THIS");
        writeLine("D=M");
        pushD();

        //push THAT
        writeLine("@THAT");
        writeLine("D=M");
        pushD();

        // ARG = SP - 5 - nArgs
        writeLine("@SP");
        writeLine("D=M");
        writeLine("@5");
        writeLine("D=D-A");
        writeLine("@" + nArgs);
        writeLine("D=D-A");
        writeLine("@ARG");
        writeLine("M=D");

        // LCL = SP
        writeLine("@SP");
        writeLine("D=M");
        writeLine("@LCL");
        writeLine("M=D");

        writeGoto(functionName);

        writeLabel(label);

        labelCounter++;
    }

    public void writeReturn() throws IOException {
        // endFrame = LCL
        writeLine("@LCL");
        writeLine("D=M");
        writeLine("@ENDFRAME");
        writeLine("M=D");

        // retAddr = RAM[endFrame - 5]
        writeLine("@5");
        writeLine("A=D-A");
        writeLine("D=M");
        writeLine("@RETADDR");
        writeLine("M=D");

        // RAM[ARG] = pop()
        writeLine("@SP");
        writeLine("AM=M-1");
        writeLine("@ARG");
        writeLine("A=M");
        writeLine("M=D");

        // SP = ARG + 1
        writeLine("@ARG");
        writeLine("D=M+1");
        writeLine("@SP");
        writeLine("M=D");

        // THAT = RAM[endFrame - 1]
        writeLine("@ENDFRAME");
        writeLine("A=M-1");
        writeLine("D=M");
        writeLine("@THAT");
        writeLine("M=D");

        // THIS = RAM[endFrame - 2]
        writeLine("@ENDFRAME");
        writeLine("D=M");
        writeLine("@2");
        writeLine("A=D-A");
        writeLine("D=M");
        writeLine("@THIS");
        writeLine("M=D");

        // ARG = RAM[endFrame - 3]
        writeLine("@ENDFRAME");
        writeLine("D=M");
        writeLine("@3");
        writeLine("A=D-A");
        writeLine("D=M");
        writeLine("@ARG");
        writeLine("M=D");

        // LCL = RAM[endFrame - 4]
        writeLine("@ENDFRAME");
        writeLine("D=M");
        writeLine("@4");
        writeLine("A=D-A");
        writeLine("D=M");
        writeLine("@LCL");
        writeLine("M=D");

        // goto retAddr
        writeLine("@RETADDR");
        writeLine("A=M");
        writeLine("0;JMP");
    }

    public void writeInit() throws IOException {
        writeLine("@256");
        writeLine("D=A");
        writeLine("@SP");
        writeLine("M=D");
        writeCall("Sys.init", 0);
    }

    public void pushD() throws IOException {
        // helper method - pushing the value D holds to the stack

        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=D");
        IncSP();
    }

    private void writeLine(String line) throws IOException {
        // helper method - to avoid writing "writer.newLine()" every time
        writer.write(line);
        writer.newLine();
        lineCounter++;
    }

    private void IncSP() throws IOException {
        // writing the hack code of SP++
        writeLine("@SP");
        writeLine("M=M+1");
    }

    private void DecSP() throws IOException {
        // writing the hack code of SP--
        writeLine("@SP");
        writeLine("M=M-1");
    }

    private String getSegmentKey(String segment) {
        if(segment.equals("local")) return "LCL";
        if(segment.equals("argument")) return "ARG";
        if(segment.equals("this")) return "THIS";
        if(segment.equals("that")) return "THAT";
        if(segment.equals("temp")) return "R5";
        else return null;
    }
}
