package main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

  @TempDir
  Path dir;

  @Test
  void writesAsmNextToVmFile() throws IOException {
    Path source = dir.resolve("Foo.vm");
    Files.writeString(source, "push constant 7\npush constant 8\nadd\n");
    Path output = Main.translate(source);
    assertEquals(dir.resolve("Foo.asm").toAbsolutePath(), output);
    String asm = Files.readString(output);
    assertTrue(asm.startsWith("// push constant 7\n"));
    assertTrue(asm.contains("// add\n"));
  }

  @Test
  void rejectsFileWithoutVmExtension() throws IOException {
    Path source = dir.resolve("Foo.txt");
    Files.writeString(source, "add\n");
    assertThrows(IllegalArgumentException.class, () -> Main.translate(source));
  }

  @Test
  void removesOutputWhenTranslationFails() throws IOException {
    Path source = dir.resolve("Bad.vm");
    Files.writeString(source, "push constant 1\nmul\n");
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> Main.translate(source));
    assertEquals("Bad.vm: line 2: unknown command 'mul'", e.getMessage());
    assertFalse(Files.exists(dir.resolve("Bad.asm")));
  }
}
