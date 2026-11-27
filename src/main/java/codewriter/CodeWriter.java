package codewriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class CodeWriter implements AutoCloseable {

  private static final int MAX_CONSTANT = 32767;
  private static final int TEMP_BASE = 5;
  private static final int TEMP_SIZE = 8;

  private final Writer out;
  private String fileName = "";
  private String currentFunction = "";
  private int labelCount = 0;

  public CodeWriter(Path file) throws IOException {
    this(Files.newBufferedWriter(file));
  }

  public CodeWriter(Writer out) {
    this.out = out;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
    this.currentFunction = fileName;
  }

  public void writeArithmetic(String command) {
    comment(command);
    switch (command) {
      case "add" -> binary("M=D+M");
      case "sub" -> binary("M=M-D");
      case "and" -> binary("M=D&M");
      case "or" -> binary("M=D|M");
      case "neg" -> unary("M=-M");
      case "not" -> unary("M=!M");
      case "eq" -> comparison("EQ");
      case "gt" -> comparison("GT");
      case "lt" -> comparison("LT");
      default -> throw new IllegalArgumentException("unknown arithmetic command '" + command + "'");
    }
  }

  public void writePush(String segment, int index) {
    comment("push " + segment + " " + index);
    switch (segment) {
      case "constant" -> write("@" + inRange(segment, index, MAX_CONSTANT), "D=A");
      case "local", "argument", "this", "that" -> write("@" + base(segment), "D=M", "@" + index, "A=D+A", "D=M");
      case "temp" -> write("@" + temp(index), "D=M");
      case "pointer" -> write("@" + pointer(index), "D=M");
      case "static" -> write("@" + fileName + "." + index, "D=M");
      default -> throw new IllegalArgumentException("unknown segment '" + segment + "'");
    }
    pushD();
  }

  public void writePop(String segment, int index) {
    comment("pop " + segment + " " + index);
    switch (segment) {
      case "local", "argument", "this", "that" -> {
        write("@" + base(segment), "D=M", "@" + index, "D=D+A", "@R13", "M=D");
        popD();
        write("@R13", "A=M", "M=D");
      }
      case "temp" -> {
        popD();
        write("@" + temp(index), "M=D");
      }
      case "pointer" -> {
        popD();
        write("@" + pointer(index), "M=D");
      }
      case "static" -> {
        popD();
        write("@" + fileName + "." + index, "M=D");
      }
      case "constant" -> throw new IllegalArgumentException("cannot pop to segment 'constant'");
      default -> throw new IllegalArgumentException("unknown segment '" + segment + "'");
    }
  }

  public void writeLabel(String label) {
    comment("label " + label);
    write("(" + qualified(label) + ")");
  }

  public void writeGoto(String label) {
    comment("goto " + label);
    write("@" + qualified(label), "0;JMP");
  }

  public void writeIf(String label) {
    comment("if-goto " + label);
    popD();
    write("@" + qualified(label), "D;JNE");
  }

  public void writeFunction(String name, int nVars) {
    comment("function " + name + " " + nVars);
    currentFunction = name;
    write("(" + name + ")");
    for (int i = 0; i < nVars; i++) {
      write("@SP", "A=M", "M=0", "@SP", "M=M+1");
    }
  }

  @Override
  public void close() {
    try {
      out.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void binary(String operation) {
    write("@SP", "AM=M-1", "D=M", "A=A-1", operation);
  }

  private void unary(String operation) {
    write("@SP", "A=M-1", operation);
  }

  private void comparison(String jump) {
    int id = labelCount++;
    String trueLabel = "TRUE_" + jump + "_" + id;
    String endLabel = "END_" + jump + "_" + id;
    write("@SP", "AM=M-1", "D=M", "A=A-1", "D=M-D");
    write("@" + trueLabel, "D;J" + jump);
    write("@SP", "A=M-1", "M=0", "@" + endLabel, "0;JMP");
    write("(" + trueLabel + ")", "@SP", "A=M-1", "M=-1");
    write("(" + endLabel + ")");
  }

  private void pushD() {
    write("@SP", "A=M", "M=D", "@SP", "M=M+1");
  }

  private void popD() {
    write("@SP", "AM=M-1", "D=M");
  }

  private String qualified(String label) {
    return currentFunction + "$" + label;
  }

  private static String base(String segment) {
    return switch (segment) {
      case "local" -> "LCL";
      case "argument" -> "ARG";
      case "this" -> "THIS";
      case "that" -> "THAT";
      default -> throw new IllegalArgumentException("segment '" + segment + "' has no base register");
    };
  }

  private static int temp(int index) {
    return TEMP_BASE + inRange("temp", index, TEMP_SIZE - 1);
  }

  private static String pointer(int index) {
    return inRange("pointer", index, 1) == 0 ? "THIS" : "THAT";
  }

  private static int inRange(String segment, int index, int max) {
    if (index > max) {
      throw new IllegalArgumentException("index " + index + " out of range for segment '" + segment + "'");
    }
    return index;
  }

  private void comment(String command) {
    write("// " + command);
  }

  private void write(String... lines) {
    try {
      for (String line : lines) {
        out.write(line);
        out.write('\n');
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
