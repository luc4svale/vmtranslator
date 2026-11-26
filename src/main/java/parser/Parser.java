package parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Parser {

  private static final Set<String> ARITHMETIC =
      Set.of("add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not");

  private record Command(CommandType type, String arg1, int arg2) {
  }

  private final List<Command> commands = new ArrayList<>();
  private int current = -1;

  public Parser(Path file) throws IOException {
    List<String> lines = Files.readAllLines(file);
    for (int i = 0; i < lines.size(); i++) {
      String line = stripComment(lines.get(i)).strip();
      if (!line.isEmpty()) {
        commands.add(parse(line.split("\\s+"), i + 1));
      }
    }
  }

  public boolean hasMoreCommands() {
    return current + 1 < commands.size();
  }

  public void advance() {
    if (!hasMoreCommands()) {
      throw new IllegalStateException("no more commands");
    }
    current++;
  }

  public CommandType commandType() {
    return command().type();
  }

  public String arg1() {
    return command().arg1();
  }

  public int arg2() {
    Command command = command();
    return switch (command.type()) {
      case C_PUSH, C_POP -> command.arg2();
      default -> throw new IllegalStateException("arg2 is not defined for " + command.type());
    };
  }

  private Command command() {
    if (current < 0) {
      throw new IllegalStateException("advance must be called before reading a command");
    }
    return commands.get(current);
  }

  private static String stripComment(String line) {
    int index = line.indexOf("//");
    return index < 0 ? line : line.substring(0, index);
  }

  private static Command parse(String[] tokens, int line) {
    String keyword = tokens[0];
    if (ARITHMETIC.contains(keyword)) {
      return withoutArgs(CommandType.C_ARITHMETIC, tokens, line);
    }
    return switch (keyword) {
      case "push" -> withNameAndNumber(CommandType.C_PUSH, tokens, line);
      case "pop" -> withNameAndNumber(CommandType.C_POP, tokens, line);
      default -> throw new IllegalArgumentException("line " + line + ": unknown command '" + keyword + "'");
    };
  }

  private static Command withoutArgs(CommandType type, String[] tokens, int line) {
    expectArgs(tokens, 0, line);
    return new Command(type, tokens[0], 0);
  }

  private static Command withNameAndNumber(CommandType type, String[] tokens, int line) {
    expectArgs(tokens, 2, line);
    return new Command(type, tokens[1], number(tokens[2], line));
  }

  private static void expectArgs(String[] tokens, int count, int line) {
    if (tokens.length != count + 1) {
      throw new IllegalArgumentException(
          "line " + line + ": '" + tokens[0] + "' expects " + count + " argument(s), got " + (tokens.length - 1));
    }
  }

  private static int number(String token, int line) {
    int value;
    try {
      value = Integer.parseInt(token);
    } catch (NumberFormatException e) {
      value = -1;
    }
    if (value < 0) {
      throw new IllegalArgumentException("line " + line + ": invalid number '" + token + "'");
    }
    return value;
  }
}
