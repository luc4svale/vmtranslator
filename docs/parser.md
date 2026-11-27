# Parser

[Voltar ao README](../README.md)

- [Funcionamento](#funcionamento)
- [Tipos de comando](#tipos-de-comando)
- [Interface](#interface)
- [Erros](#erros)
- [Testes](#testes)
- [Diferenças em relação ao tutorial](#diferenças-em-relação-ao-tutorial)

## Funcionamento

O `Parser` lê um arquivo `.vm` e entrega um comando por vez, já decomposto em tipo e argumentos. O código fica no pacote `parser`, em `src/main/java/parser/`. O `Parser.java` contém o leitor, e o `CommandType.java` define os tipos de comando.

O construtor lê o arquivo inteiro de uma vez. Em cada linha, ele corta o `//` e tudo o que vem depois, descarta os espaços das pontas e pula as linhas que ficam vazias. Assim o comentário no fim da linha, como em `push constant 0 // sum = 0`, some junto com os comentários de linha inteira. O que sobra é quebrado em palavras por qualquer sequência de espaços ou tabulações.

Cada comando vira um registro interno com três campos, o tipo, o primeiro argumento e o segundo argumento. O `Parser` confere a quantidade de argumentos e converte o índice para inteiro ainda no construtor, e por isso um erro de leitura aparece antes de qualquer Assembly ser gerado.

A leitura aceita fim de linha CRLF, o formato dos `.vm` que vêm no pacote de software do Nand2Tetris.

## Tipos de comando

| Tipo | Comando | `arg1()` | `arg2()` |
| --- | --- | --- | --- |
| `C_ARITHMETIC` | `add`, `sub`, `neg`, `eq`, `gt`, `lt`, `and`, `or`, `not` | o próprio comando | não existe |
| `C_PUSH` | `push segmento i` | o segmento | o índice |
| `C_POP` | `pop segmento i` | o segmento | o índice |
| `C_LABEL` | `label XYZ` | o rótulo | não existe |
| `C_GOTO` | `goto XYZ` | o rótulo | não existe |
| `C_IF` | `if-goto XYZ` | o rótulo | não existe |
| `C_FUNCTION` | `function f n` | o nome da função | o número de variáveis locais |
| `C_CALL` | `call f n` | o nome da função | o número de argumentos |
| `C_RETURN` | `return` | não existe | não existe |

Os seis últimos tipos entraram na segunda parte. O `Parser` ganhou três linhas no `switch` para os desvios e três para as funções, sem mudar o que já lia os comandos da primeira parte.

## Interface

| Método | O que faz |
| --- | --- |
| `Parser(arquivo)` | Lê o arquivo e decompõe todos os comandos |
| `hasMoreCommands()` | Indica se ainda há comandos |
| `advance()` | Passa para o próximo comando |
| `commandType()` | Devolve o tipo do comando corrente |
| `arg1()` | Devolve o primeiro argumento |
| `arg2()` | Devolve o segundo argumento, como inteiro |

O `Main` chama `advance()` antes de ler cada comando. Pedir o tipo antes do primeiro `advance()`, avançar depois do último comando ou pedir um argumento que o tipo não tem gera um `IllegalStateException`, porque esses casos são erro de uso da classe, e não do arquivo de entrada.

## Erros

Os erros do arquivo de entrada saem como `IllegalArgumentException`, com o número da linha. O `Main` acrescenta o nome do arquivo antes da mensagem.

| Situação | Mensagem |
| --- | --- |
| Comando desconhecido | `line 2: unknown command 'mul'` |
| Quantidade errada de argumentos | `line 5: 'push' expects 2 argument(s), got 1` |
| Índice que não é um inteiro não negativo | `line 3: invalid number 'x'` |

## Testes

Os testes ficam em `src/test/java/parser/ParserTest.java`. Cada teste grava um `.vm` pequeno numa pasta temporária e lê o arquivo com o `Parser`.

| Teste | O que confere |
| --- | --- |
| `skipsBlankLinesAndComments` | Linhas vazias, comentários de linha inteira, comentário no fim da linha e tabulação |
| `readsArithmeticCommand` | Tipo e `arg1` de um comando aritmético, e `arg2` recusado |
| `readsPushAndPop` | Tipo, segmento e índice de `push` e `pop` |
| `readsBranchingCommands` | `label`, `goto` e `if-goto` com o rótulo em `arg1` |
| `readsFunctionCommands` | `function` e `call` com nome e número, e `return` sem argumentos |
| `acceptsWindowsLineEndings` | Arquivo com fim de linha CRLF |
| `rejectsUnknownCommand` | Mensagem de comando desconhecido com o número da linha |
| `rejectsWrongArgumentCount` | Comandos com argumentos a mais ou a menos |
| `rejectsInvalidIndex` | Índice com letras e índice negativo |
| `requiresAdvanceBeforeReading` | Leitura antes do primeiro `advance()` e avanço depois do último comando |

## Diferenças em relação ao tutorial

O capítulo 12 do tutorial descreve o `Parser` em termos gerais, sem código. A implementação da LambdaGeo usa um tipo por comando da VM, como um valor para `add` e outro para `sub`. Este `Parser` usa as nove categorias da tabela acima, e o comando aritmético vai em `arg1()`.

O comando decomposto fica num registro privado do Java, e a leitura do arquivo inteiro no construtor permite acusar os erros de entrada com o número da linha antes de começar a tradução.
