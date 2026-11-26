package codewriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class CodeWriter implements AutoCloseable {

  private static final int MAX_CONSTANT = 32767;

  private final Writer out;
  private String fileName = "";

  public CodeWriter(Path file) throws IOException {
    this(Files.newBufferedWriter(file));
  }

  public CodeWriter(Writer out) {
    this.out = out;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
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
      default -> throw new IllegalArgumentException("unknown arithmetic command '" + command + "'");
    }
  }

  public void writePush(String segment, int index) {
    comment("push " + segment + " " + index);
    switch (segment) {
      case "constant" -> write("@" + inRange(segment, index, MAX_CONSTANT), "D=A");
      default -> throw new IllegalArgumentException("unknown segment '" + segment + "'");
    }
    pushD();
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

  private void pushD() {
    write("@SP", "A=M", "M=D", "@SP", "M=M+1");
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
