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
  void translatesAllVmFilesOfDirectoryIntoOneAsm() throws IOException {
    Path program = Files.createDirectory(dir.resolve("Prog"));
    Files.writeString(program.resolve("Main.vm"), "push static 0\n");
    Files.writeString(program.resolve("Ball.vm"), "push static 0\n");
    Files.writeString(program.resolve("notes.txt"), "ignored\n");
    Path output = Main.translate(program);
    assertEquals(program.resolve("Prog.asm").toAbsolutePath(), output);
    String asm = Files.readString(output);
    assertTrue(asm.indexOf("@Ball.0") < asm.indexOf("@Main.0"));
    assertFalse(asm.contains("ignored"));
  }

  @Test
  void writesBootstrapOnlyForDirectoryWithSysVm() throws IOException {
    Path withSys = Files.createDirectory(dir.resolve("WithSys"));
    Files.writeString(withSys.resolve("Sys.vm"), "function Sys.init 0\nlabel END\ngoto END\n");
    assertTrue(Files.readString(Main.translate(withSys)).startsWith("// bootstrap\n@256\n"));

    Path withoutSys = Files.createDirectory(dir.resolve("WithoutSys"));
    Files.writeString(withoutSys.resolve("Main.vm"), "function Main.main 0\npush constant 0\nreturn\n");
    assertFalse(Files.readString(Main.translate(withoutSys)).contains("bootstrap"));

    assertFalse(Files.readString(Main.translate(withSys.resolve("Sys.vm"))).contains("bootstrap"));
  }

  @Test
  void rejectsDirectoryWithoutVmFiles() throws IOException {
    Path empty = Files.createDirectory(dir.resolve("Empty"));
    assertThrows(IllegalArgumentException.class, () -> Main.translate(empty));
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
