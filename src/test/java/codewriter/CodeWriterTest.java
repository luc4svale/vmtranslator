package codewriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  void subKeepsOperandOrder() {
    assertEquals("""
        // sub
        @SP
        AM=M-1
        D=M
        A=A-1
        M=M-D
        """, translate(w -> w.writeArithmetic("sub")));
  }

  @Test
  void binaryLogicalCommands() {
    assertTrue(translate(w -> w.writeArithmetic("and")).endsWith("A=A-1\nM=D&M\n"));
    assertTrue(translate(w -> w.writeArithmetic("or")).endsWith("A=A-1\nM=D|M\n"));
  }

  @Test
  void unaryCommands() {
    assertEquals("""
        // neg
        @SP
        A=M-1
        M=-M
        """, translate(w -> w.writeArithmetic("neg")));
    assertEquals("""
        // not
        @SP
        A=M-1
        M=!M
        """, translate(w -> w.writeArithmetic("not")));
  }

  @Test
  void eq() {
    assertEquals("""
        // eq
        @SP
        AM=M-1
        D=M
        A=A-1
        D=M-D
        @TRUE_EQ_0
        D;JEQ
        @SP
        A=M-1
        M=0
        @END_EQ_0
        0;JMP
        (TRUE_EQ_0)
        @SP
        A=M-1
        M=-1
        (END_EQ_0)
        """, translate(w -> w.writeArithmetic("eq")));
  }

  @Test
  void comparisonsUseUniqueLabels() {
    String asm = translate(w -> {
      w.writeArithmetic("eq");
      w.writeArithmetic("gt");
      w.writeArithmetic("lt");
      w.writeArithmetic("eq");
    });
    assertTrue(asm.contains("(TRUE_EQ_0)"));
    assertTrue(asm.contains("@TRUE_GT_1\nD;JGT\n"));
    assertTrue(asm.contains("@TRUE_LT_2\nD;JLT\n"));
    assertTrue(asm.contains("(END_EQ_3)"));
  }

  @Test
  void rejectsUnknownCommandAndSegment() {
    assertThrows(IllegalArgumentException.class, () -> translate(w -> w.writeArithmetic("mul")));
    assertThrows(IllegalArgumentException.class, () -> translate(w -> w.writePush("heap", 0)));
    assertThrows(IllegalArgumentException.class, () -> translate(w -> w.writePush("constant", 32768)));
  }
}
