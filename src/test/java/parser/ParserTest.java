package parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParserTest {

  @TempDir
  Path dir;

  private Parser parse(String source) throws IOException {
    Path file = dir.resolve("Test.vm");
    Files.writeString(file, source);
    return new Parser(file);
  }

  @Test
  void skipsBlankLinesAndComments() throws IOException {
    Parser parser = parse("""
        // full line comment

           push constant 7   // trailing comment
        \tadd
        """);
    assertTrue(parser.hasMoreCommands());
    parser.advance();
    assertEquals(CommandType.C_PUSH, parser.commandType());
    parser.advance();
    assertEquals(CommandType.C_ARITHMETIC, parser.commandType());
    assertFalse(parser.hasMoreCommands());
  }

  @Test
  void readsArithmeticCommand() throws IOException {
    Parser parser = parse("sub\n");
    parser.advance();
    assertEquals(CommandType.C_ARITHMETIC, parser.commandType());
    assertEquals("sub", parser.arg1());
    assertThrows(IllegalStateException.class, parser::arg2);
  }

  @Test
  void readsPushAndPop() throws IOException {
    Parser parser = parse("push local 2\npop static 13\n");
    parser.advance();
    assertEquals(CommandType.C_PUSH, parser.commandType());
    assertEquals("local", parser.arg1());
    assertEquals(2, parser.arg2());
    parser.advance();
    assertEquals(CommandType.C_POP, parser.commandType());
    assertEquals("static", parser.arg1());
    assertEquals(13, parser.arg2());
  }

  @Test
  void acceptsWindowsLineEndings() throws IOException {
    Parser parser = parse("push constant 7\r\nadd\r\n");
    parser.advance();
    assertEquals(7, parser.arg2());
    parser.advance();
    assertEquals("add", parser.arg1());
  }

  @Test
  void rejectsUnknownCommand() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> parse("push constant 1\nmul\n"));
    assertEquals("line 2: unknown command 'mul'", e.getMessage());
  }

  @Test
  void rejectsWrongArgumentCount() {
    assertThrows(IllegalArgumentException.class, () -> parse("push constant\n"));
    assertThrows(IllegalArgumentException.class, () -> parse("add 1\n"));
  }

  @Test
  void rejectsInvalidIndex() {
    assertThrows(IllegalArgumentException.class, () -> parse("push constant x\n"));
    assertThrows(IllegalArgumentException.class, () -> parse("push constant -1\n"));
  }

  @Test
  void requiresAdvanceBeforeReading() throws IOException {
    Parser parser = parse("add\n");
    assertThrows(IllegalStateException.class, parser::commandType);
    parser.advance();
    assertThrows(IllegalStateException.class, parser::advance);
  }
}
