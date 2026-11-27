package main;

import codewriter.CodeWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import parser.Parser;

public class Main {

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("usage: java -jar vmtranslator.jar <file.vm>");
      System.exit(2);
    }
    try {
      System.out.println(translate(Path.of(args[0])));
    } catch (IOException | UncheckedIOException | IllegalArgumentException | IllegalStateException e) {
      System.err.println("error: " + e.getMessage());
      System.exit(1);
    }
  }

  public static Path translate(Path input) throws IOException {
    Path source = input.toAbsolutePath().normalize();
    if (!Files.isRegularFile(source) || !source.getFileName().toString().endsWith(".vm")) {
      throw new IllegalArgumentException("input must be a .vm file: " + input);
    }
    Path output = source.resolveSibling(baseName(source) + ".asm");
    try (CodeWriter writer = new CodeWriter(output)) {
      translateFile(source, writer);
    } catch (IOException | RuntimeException e) {
      Files.deleteIfExists(output);
      throw e;
    }
    return output;
  }

  private static void translateFile(Path file, CodeWriter writer) throws IOException {
    try {
      writer.setFileName(baseName(file));
      Parser parser = new Parser(file);
      while (parser.hasMoreCommands()) {
        parser.advance();
        switch (parser.commandType()) {
          case C_ARITHMETIC -> writer.writeArithmetic(parser.arg1());
          case C_PUSH -> writer.writePush(parser.arg1(), parser.arg2());
          case C_POP -> writer.writePop(parser.arg1(), parser.arg2());
          case C_LABEL -> writer.writeLabel(parser.arg1());
          case C_GOTO -> writer.writeGoto(parser.arg1());
          case C_IF -> writer.writeIf(parser.arg1());
          case C_FUNCTION -> writer.writeFunction(parser.arg1(), parser.arg2());
          case C_CALL -> writer.writeCall(parser.arg1(), parser.arg2());
          case C_RETURN -> writer.writeReturn();
          default -> throw new IllegalArgumentException("unsupported command " + parser.commandType());
        }
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(file.getFileName() + ": " + e.getMessage(), e);
    }
  }

  private static String baseName(Path file) {
    String name = file.getFileName().toString();
    return name.substring(0, name.length() - ".vm".length());
  }
}
