# CodeWriter

[Voltar ao README](../README.md)

- [Funcionamento](#funcionamento)
- [Interface](#interface)
- [Acesso à memória](#acesso-à-memória)
- [Comandos aritméticos e lógicos](#comandos-aritméticos-e-lógicos)
- [Desvios](#desvios)
- [Funções](#funções)
- [Código de inicialização](#código-de-inicialização)
- [Símbolos gerados](#símbolos-gerados)
- [Erros](#erros)
- [Testes](#testes)
- [Diferenças em relação ao tutorial](#diferenças-em-relação-ao-tutorial)

## Funcionamento

O `CodeWriter` gera o Assembly Hack de cada comando da VM. O código fica em `src/main/java/codewriter/CodeWriter.java`.

A classe guarda o estado de que a tradução precisa. O nome do arquivo corrente monta os símbolos de `static`. O nome da função corrente monta os rótulos de `label`, `goto`, `if-goto` e `call`. Dois contadores do programa inteiro numeram os rótulos das comparações e os rótulos de retorno.

Antes do Assembly de cada comando, o `CodeWriter` escreve o próprio comando VM como comentário, como em `// push constant 7`. O Assembler descarta os comentários, e no CPU Emulator eles mostram qual comando gerou cada trecho.

O `CodeWriter` escreve num `Writer` do Java. O `Main` passa um arquivo, e os testes passam um `StringWriter` e comparam o texto gerado com o Assembly esperado.

A RAM do Hack tem papéis fixos nas primeiras posições. `RAM[0]` a `RAM[4]` guardam `SP`, `LCL`, `ARG`, `THIS` e `THAT`, `RAM[5]` a `RAM[12]` formam o segmento `temp`, e `RAM[13]` a `RAM[15]` são os registradores de rascunho `R13`, `R14` e `R15`. As variáveis `static` começam em `RAM[16]`, e a pilha em `RAM[256]`.

Três trechos se repetem em quase todos os comandos.

| Trecho | Assembly | Efeito |
| --- | --- | --- |
| Empilhar `D` | `@SP`, `A=M`, `M=D`, `@SP`, `M=M+1` | Grava `D` no topo e avança o `SP` |
| Desempilhar para `D` | `@SP`, `AM=M-1`, `D=M` | Recua o `SP` e lê o valor do topo |
| Empilhar um registrador | `@LCL`, `D=M` e o trecho de empilhar `D` | Empilha o conteúdo de `LCL`, `ARG`, `THIS` ou `THAT` |

## Interface

| Método | Comando VM |
| --- | --- |
| `CodeWriter(arquivo)` | Abre o `.asm` de saída |
| `setFileName(nome)` | Informa o arquivo `.vm` corrente |
| `writeArithmetic(comando)` | `add`, `sub`, `neg`, `eq`, `gt`, `lt`, `and`, `or`, `not` |
| `writePush(segmento, índice)` | `push` |
| `writePop(segmento, índice)` | `pop` |
| `writeLabel(rótulo)` | `label` |
| `writeGoto(rótulo)` | `goto` |
| `writeIf(rótulo)` | `if-goto` |
| `writeFunction(nome, nVars)` | `function` |
| `writeCall(nome, nArgs)` | `call` |
| `writeReturn()` | `return` |
| `writeInit()` | Código de inicialização |
| `close()` | Fecha o arquivo |

## Acesso à memória

| Segmento | Endereço de `segmento i` |
| --- | --- |
| `constant` | Nenhum. O valor é o próprio `i`, e só existe `push` |
| `local`, `argument`, `this`, `that` | `RAM[base + i]`, com a base em `LCL`, `ARG`, `THIS` ou `THAT` |
| `temp` | `RAM[5 + i]`, com `i` de 0 a 7 |
| `pointer` | `THIS` para `pointer 0` e `THAT` para `pointer 1` |
| `static` | O símbolo `Arquivo.i`, como `Foo.3` para `static 3` em `Foo.vm` |

O `push constant` carrega o literal com `D=A`, porque não há memória a ler.

```
// push constant 17
@17
D=A
@SP
A=M
M=D
@SP
M=M+1
```

Nos segmentos com registrador base, o `push` soma o índice à base e lê a posição.

```
// push local 2
@LCL
D=M
@2
A=D+A
D=M
@SP
A=M
M=D
@SP
M=M+1
```

O `pop` precisa do endereço e do valor ao mesmo tempo, e os dois passariam por `D`. Por isso o endereço fica em `R13` enquanto o valor sai da pilha.

```
// pop local 2
@LCL
D=M
@2
D=D+A
@R13
M=D
@SP
AM=M-1
D=M
@R13
A=M
M=D
```

Em `temp`, `pointer` e `static`, o endereço já é conhecido na tradução e vai direto na instrução de endereço, sem `R13`.

```
// pop temp 6
@SP
AM=M-1
D=M
@11
M=D
```

Um `pop pointer 0` grava em `THIS` e muda a base do segmento `this`. Os acessos seguintes a `this i` já usam a base nova.

## Comandos aritméticos e lógicos

Os comandos binários desempilham o segundo operando e gravam o resultado no lugar do primeiro, sem avançar o `SP` de novo. Só a última linha muda entre eles.

```
// add
@SP
AM=M-1
D=M
A=A-1
M=D+M
```

| Comando | Última linha |
| --- | --- |
| `add` | `M=D+M` |
| `sub` | `M=M-D` |
| `and` | `M=D&M` |
| `or` | `M=D\|M` |

Na hora da última linha, `D` tem o segundo operando e `M` o primeiro. Por isso `sub` usa `M=M-D`, que calcula `x - y`.

`neg` e `not` mudam o topo no lugar, com `@SP`, `A=M-1` e `M=-M` ou `M=!M`.

O Hack não tem instrução de comparar. A comparação calcula `x - y`, salta conforme o resultado e grava -1 para verdadeiro ou 0 para falso.

```
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
```

`gt` e `lt` trocam `JEQ` por `JGT` e `JLT`. Cada comparação recebe um número novo do contador, e assim o Assembler não encontra o mesmo rótulo duas vezes.

## Desvios

O rótulo `XYZ` dentro da função `f` vira `f$XYZ`, e duas funções podem ter o seu próprio `label LOOP` sem conflito. Antes do primeiro `function` de um arquivo, a função corrente é o nome do arquivo.

```
// label LOOP
(Main.loop$LOOP)
// goto LOOP
@Main.loop$LOOP
0;JMP
// if-goto LOOP
@SP
AM=M-1
D=M
@Main.loop$LOOP
D;JNE
```

O `if-goto` desempilha a condição e salta quando ela é diferente de zero.

## Funções

O `function f n` declara o rótulo `(f)` e empilha `n` zeros, um bloco de cinco instruções por variável local. Com `n` igual a zero, sai só o rótulo. Como o `call` faz `LCL = SP`, os zeros ocupam `local 0` a `local n-1`.

```
// function Foo.bar 2
(Foo.bar)
@SP
A=M
M=0
@SP
M=M+1
@SP
A=M
M=0
@SP
M=M+1
```

O `call f n` monta o frame de quem chama, nesta ordem.

1. Empilha o endereço do rótulo de retorno, com `D=A`.
2. Empilha o conteúdo de `LCL`, `ARG`, `THIS` e `THAT`, com `D=M`.
3. Faz `ARG = SP - 5 - n`, que aponta para o primeiro argumento.
4. Faz `LCL = SP`.
5. Salta para `f` e declara o rótulo de retorno logo depois.

```
// call Bar.mult 2
@Foo.main$ret.0
D=A
@SP
A=M
M=D
@SP
M=M+1
@LCL
D=M
@SP
A=M
M=D
@SP
M=M+1
...
@5
D=A
@2
D=D+A
@SP
D=M-D
@ARG
M=D
@SP
D=M
@LCL
M=D
@Bar.mult
0;JMP
(Foo.main$ret.0)
```

O trecho omitido repete o bloco de `LCL` para `ARG`, `THIS` e `THAT`. São 49 instruções por chamada.

Logo depois do `call`, o topo da pilha fica assim.

| Posição | Conteúdo |
| --- | --- |
| `SP` e `LCL` | Próxima posição livre, onde começa `local 0` |
| `LCL - 1` | `THAT` de quem chamou |
| `LCL - 2` | `THIS` de quem chamou |
| `LCL - 3` | `ARG` de quem chamou |
| `LCL - 4` | `LCL` de quem chamou |
| `LCL - 5` | Endereço de retorno |
| `ARG` a `ARG + n - 1` | Os argumentos, empilhados por quem chamou |

O `return` desfaz o frame, com `R14` para o fim do frame e `R15` para o endereço de retorno.

1. Guarda `LCL` em `R14`, porque `LCL` vai mudar no passo 5.
2. Lê o endereço de retorno em `R14 - 5` e guarda em `R15`.
3. Desempilha o valor de retorno e grava em `*ARG`.
4. Faz `SP = ARG + 1`.
5. Restaura `THAT`, `THIS`, `ARG` e `LCL` a partir de `R14 - 1` a `R14 - 4`.
6. Salta para o endereço guardado em `R15`.

O passo 2 vem antes do passo 3 por um motivo. Numa função chamada com zero argumentos, `ARG` aponta para a posição do endereço de retorno, e gravar o valor primeiro apagaria esse endereço. O `ARG` só é restaurado no passo 5 porque, até ali, ele indica onde o valor de retorno deve ficar.

```
// return
@LCL
D=M
@R14
M=D
@5
D=A
@R14
A=M-D
D=M
@R15
M=D
@SP
AM=M-1
D=M
@ARG
A=M
M=D
@ARG
D=M+1
@SP
M=D
@1
D=A
@R14
A=M-D
D=M
@THAT
M=D
...
@R15
A=M
0;JMP
```

O trecho omitido repete a restauração de `THAT` para `THIS`, `ARG` e `LCL`, com os deslocamentos 2, 3 e 4. São 52 instruções.

## Código de inicialização

O `writeInit` põe a pilha em `RAM[256]` e faz uma chamada completa a `Sys.init`, sem argumentos.

```
// bootstrap
@256
D=A
@SP
M=D
// call Sys.init 0
@Bootstrap$ret.0
...
```

O `call` empilha os cinco valores do frame, e `Sys.init` começa com `SP` e `LCL` em 261 e `ARG` em 256. Os testes FibonacciElement e StaticsTest conferem esses valores. Como `Sys.init` nunca retorna, o endereço de retorno empilhado nunca é usado, mas o frame deixa a pilha no formato que o resto do programa espera.

O `Main` chama o `writeInit` só quando a entrada é um diretório com um arquivo `Sys.vm`. Os testes BasicLoop, FibonacciSeries e SimpleFunction preparam a memória pelo próprio `.tst` e falhariam com o código de inicialização.

## Símbolos gerados

| Símbolo | Uso |
| --- | --- |
| `SP`, `LCL`, `ARG`, `THIS`, `THAT` | Símbolos pré-definidos do Hack, de `RAM[0]` a `RAM[4]` |
| `R13`, `R14`, `R15` | Rascunho do `pop` e do `return` |
| `Arquivo.i` | Variável `static i` do arquivo `Arquivo.vm` |
| `função$rótulo` | Um `label rótulo` dentro de `função` |
| `função` | Ponto de entrada de uma função |
| `função$ret.i` | Endereço de retorno da chamada número `i`, feita dentro de `função` |
| `Bootstrap$ret.0` | Endereço de retorno da chamada a `Sys.init` no código de inicialização |
| `TRUE_EQ_i` e `END_EQ_i` | Rótulos da comparação número `i`, com `GT` e `LT` no lugar de `EQ` nas outras comparações |

## Erros

| Situação | Mensagem |
| --- | --- |
| Comando aritmético desconhecido | `unknown arithmetic command 'mul'` |
| Segmento desconhecido | `unknown segment 'heap'` |
| `pop` em `constant` | `cannot pop to segment 'constant'` |
| Constante acima de 32767 | `index 32768 out of range for segment 'constant'` |
| `temp` acima de 7 ou `pointer` acima de 1 | `index 8 out of range for segment 'temp'` |

O limite de `constant` vem da instrução de endereço do Hack, que carrega no máximo 15 bits. O de `temp` evita que `temp 8` caia em `R13`, que o próprio tradutor usa.

## Testes

Os testes ficam em `src/test/java/codewriter/CodeWriterTest.java`. A maioria compara o texto gerado com o Assembly exato do tutorial.

| Teste | O que confere |
| --- | --- |
| `pushConstant` | Assembly de `push constant` |
| `add` | Assembly de `add` |
| `subKeepsOperandOrder` | `sub` com `M=M-D` |
| `binaryLogicalCommands` | Última linha de `and` e `or` |
| `unaryCommands` | Assembly de `neg` e `not` |
| `eq` | Assembly completo de `eq` |
| `comparisonsUseUniqueLabels` | Contador dos rótulos em quatro comparações seguidas |
| `pushLocal` | Assembly de `push local` |
| `popLocalUsesR13` | Assembly de `pop local` com `R13` |
| `dynamicSegmentsUseTheirBaseRegisters` | `ARG`, `THIS` e `THAT` como base |
| `tempUsesFixedAddress` | Endereços de `temp` e o limite de 7 |
| `pointerAccessesThisAndThat` | `pointer 0` e `pointer 1` e o limite de 1 |
| `staticUsesFileNameAsPrefix` | Símbolo `Arquivo.i` com dois nomes de arquivo |
| `branchingOutsideFunctionUsesFileName` | `label`, `goto` e `if-goto` antes de qualquer `function` |
| `functionPushesZeroForEachLocal` | `function` com duas variáveis locais e com nenhuma |
| `labelsInsideFunctionUseFunctionName` | O mesmo rótulo em duas funções |
| `callSavesFrameAndJumps` | Assembly do `call` |
| `callsGetUniqueReturnLabels` | Rótulos de retorno em três chamadas e duas funções |
| `initSetsStackPointerAndCallsSysInit` | Começo e fim do código de inicialização |
| `returnFollowsChapterSequence` | Assembly completo do `return` |
| `returnReadsReturnAddressBeforeWritingResult` | Ordem entre o passo 2 e o passo 3 do `return` |
| `rejectsPopConstant` | Erro de `pop constant` |
| `rejectsUnknownCommandAndSegment` | Erros de comando, segmento e constante acima do limite |

Os testes do `Main`, em `src/test/java/main/MainTest.java`, conferem o nome e a pasta do `.asm`, a tradução de um diretório com os arquivos em ordem alfabética, o código de inicialização só com `Sys.vm` num diretório, a recusa de entradas inválidas e a remoção do `.asm` incompleto depois de um erro.

## Diferenças em relação ao tutorial

O capítulo 12 descreve um único `writePushPop`. O `CodeWriter` tem `writePush` e `writePop` separados.

O capítulo 13 sugere um contador de rótulos de retorno por função. O `CodeWriter` usa um contador do programa inteiro, no mesmo formato `função$ret.i`. A unicidade do rótulo não depende do nome da função, e o código de inicialização usa o prefixo `Bootstrap`.

O capítulo 13 aceita qualquer prefixo fixo para os rótulos fora de função. O `CodeWriter` usa o nome do arquivo.

O Assembly de cada comando segue o do tutorial, e a contagem de instruções bate com a tabela de custos do capítulo 12 e com as 49 instruções do `call` e as 52 do `return` do capítulo 13.
