package codewriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringWriter;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class CodeWriterTest {

  private static String translate(Consumer<CodeWriter> commands) {
    StringWriter out = new StringWriter();
    try (CodeWriter writer = new CodeWriter(out)) {
      writer.setFileName("Foo");
      commands.accept(writer);
    }
    return out.toString();
  }

  @Test
  void pushConstant() {
    assertEquals("""
        // push constant 17
        @17
        D=A
        @SP
        A=M
        M=D
        @SP
        M=M+1
        """, translate(w -> w.writePush("constant", 17)));
  }

  @Test
  void add() {
    assertEquals("""
        // add
        @SP
        AM=M-1
        D=M
        A=A-1
        M=D+M
        """, translate(w -> w.writeArithmetic("add")));
  }

  @Test
  void rejectsUnknownCommandAndSegment() {
    assertThrows(IllegalArgumentException.class, () -> translate(w -> w.writeArithmetic("mul")));
    assertThrows(IllegalArgumentException.class, () -> translate(w -> w.writePush("heap", 0)));
    assertThrows(IllegalArgumentException.class, () -> translate(w -> w.writePush("constant", 32768)));
  }
}
