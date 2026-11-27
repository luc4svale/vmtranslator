# vmtranslator

Tradutor da linguagem VM escrito em Java. VM é a linguagem da máquina virtual do curso Nand2Tetris, para a qual os programas Jack são compilados. O tradutor lê programas VM e gera código Assembly para o Hack, o computador construído no Nand2Tetris. Esse código roda no CPU Emulator, o simulador oficial do Hack.

O projeto segue o tutorial do livro didático da disciplina de Compiladores do curso de Engenharia da Computação da Universidade Federal do Maranhão. A construção passou por duas partes. A primeira traduz o acesso à memória e os comandos aritméticos e lógicos. A segunda traduz os desvios, as funções e o código de inicialização, e passa a aceitar um diretório inteiro como entrada. Este README descreve o tradutor completo e aponta para a documentação detalhada de cada módulo.

## Sumário

- [Autores](#autores)
- [Material de referência](#material-de-referência)
- [Requisitos](#requisitos)
- [Arquivos de build](#arquivos-de-build)
- [Como compilar e executar](#como-compilar-e-executar)
- [Como rodar no CPU Emulator](#como-rodar-no-cpu-emulator)
- [Funcionalidades](#funcionalidades)
- [Testes e validação](#testes-e-validação)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Documentação detalhada](#documentação-detalhada)
- [Decisões de projeto](#decisões-de-projeto)
- [Limitações conhecidas](#limitações-conhecidas)
- [Desafios e aprendizados](#desafios-e-aprendizados)
- [Uso de inteligência artificial](#uso-de-inteligência-artificial)
- [Vídeo de demonstração](#vídeo-de-demonstração)
- [Licença](#licença)

## Autores

- ANTONIO LUCAS DA SILVA VALE | 20250013480
- HIGOR PINHEIRO COSTA | 20250013695

## Material de referência

Três fontes aparecem ao longo deste repositório.

A primeira é o livro didático da disciplina, *Compiladores com Nand2Tetris*, escrito pelo grupo LambdaGeo da UFMA e publicado em [lambdageo-edu.github.io](https://lambdageo-edu.github.io). Os capítulos 12 e 13 trazem a teoria do tradutor, o pseudocódigo e o Assembly de cada comando da VM. Neste repositório, a palavra tutorial se refere a esses dois capítulos.

A segunda é o livro em que o tutorial se baseia, *The Elements of Computing Systems*, de Noam Nisan e Shimon Schocken, publicado pela MIT Press. O livro é a base do curso Nand2Tetris e divide o conteúdo em projetos numerados. O Project 7 constrói a primeira metade do tradutor, com a memória e a aritmética, e o Project 8 constrói a segunda, com os desvios e as funções. Os capítulos 7 e 8 do livro tratam desses dois projetos.

A terceira é o pacote de software que os autores do livro distribuem no site [nand2tetris.org](https://www.nand2tetris.org/). O pacote traz os programas de teste dos Projects 7 e 8 e as ferramentas oficiais, entre elas o CPU Emulator.

A dupla também consultou a implementação [lambdageo-edu/vmtranslator-java](https://github.com/lambdageo-edu/vmtranslator-java), um tradutor em Java publicado pelo mesmo grupo LambdaGeo, e comparou com ela o Assembly gerado.

## Requisitos

O projeto usa Java 17 ou superior. A dupla escolheu Java porque já tinha usado a linguagem no [jackcompiler](https://github.com/higorcos/jackcompiler), o compilador da linguagem Jack que os dois escreveram nas unidades anteriores da disciplina, e pôde reaproveitar a organização de build, testes e validação daquele projeto.

Não é preciso instalar o Maven, porque o repositório traz o Maven Wrapper, que baixa o Maven 3.9.16 na primeira execução. No Linux e no macOS, os comandos usam o script `./mvnw`. No Windows, o equivalente é o `mvnw.cmd`. O script de validação, `scripts/validate.sh`, precisa de um terminal bash, como o Git Bash ou o WSL no Windows.

## Arquivos de build

| Arquivo | Papel |
| --- | --- |
| `pom.xml` | Configura o Maven. Define o Java 17, a dependência do JUnit 5 para os testes e o plugin que gera o `target/vmtranslator.jar` com a classe `main.Main` como ponto de entrada |
| `mvnw` e `mvnw.cmd` | Os scripts do Maven Wrapper, para Linux e macOS e para Windows |
| `.mvn/wrapper/` | A configuração do Maven Wrapper, com a versão do Maven que ele baixa |
| `.gitignore` | Deixa fora do Git as classes compiladas, as saídas geradas, os arquivos de IDE e o arquivo de estado do CPU Emulator |
| `.gitattributes` | Mantém os scripts com fim de linha LF e preserva sem conversão os arquivos de teste e das ferramentas |

## Como compilar e executar

O `package` roda os testes antes de montar o jar.

```bash
./mvnw package
java -jar target/vmtranslator.jar <arquivo.vm | diretório>
```

| Entrada | Comando | Saída |
| --- | --- | --- |
| Arquivo | `java -jar target/vmtranslator.jar caminho/Xxx.vm` | `caminho/Xxx.asm` |
| Diretório | `java -jar target/vmtranslator.jar caminho/Xxx` | `caminho/Xxx/Xxx.asm` |

O tradutor grava o `.asm` na mesma pasta da entrada e imprime o caminho completo do arquivo gerado. Para não misturar arquivos gerados com os testes oficiais, os exemplos abaixo copiam cada teste para `output/`, uma pasta que o Git ignora.

### Traduzir um arquivo

```bash
mkdir -p output/SimpleAdd
cp src/test/resources/projects/7/StackArithmetic/SimpleAdd/* output/SimpleAdd/
java -jar target/vmtranslator.jar output/SimpleAdd/SimpleAdd.vm
```

Antes do Assembly de cada comando, o tradutor escreve o próprio comando VM como comentário. O começo do `SimpleAdd.asm` fica assim.

```
// push constant 7
@7
D=A
@SP
A=M
M=D
@SP
M=M+1
```

### Traduzir um diretório

Um programa da VM costuma ter vários arquivos, um por classe. Com um diretório, o tradutor lê todos os `.vm` dele em ordem alfabética, sem entrar em subpastas, e grava um único `.asm` com o nome do diretório. Quando o diretório tem um `Sys.vm`, o `.asm` começa com o código de inicialização, que põe a pilha em `RAM[256]` e chama `Sys.init`.

```bash
mkdir -p output/FibonacciElement
cp src/test/resources/projects/8/FunctionCalls/FibonacciElement/* output/FibonacciElement/
java -jar target/vmtranslator.jar output/FibonacciElement
```

### Erros

Diante de um erro, o tradutor mostra o arquivo e o problema, termina com código 1 e apaga o `.asm` incompleto. Os erros de leitura mostram também a linha.

```
error: Bad.vm: line 2: unknown command 'mul'
error: Bad.vm: line 5: 'push' expects 2 argument(s), got 1
error: Bad.vm: cannot pop to segment 'constant'
error: Bad.vm: index 8 out of range for segment 'temp'
```

Um caminho que não existe, ou um arquivo sem a extensão `.vm`, gera a mensagem `input must be a .vm file or a directory`. Um diretório sem arquivos `.vm` gera `no .vm files in directory`.

## Como rodar no CPU Emulator

A pasta `tools/` traz o CPU Emulator do pacote de software do Nand2Tetris, e o script `tools/CPUEmulator.sh` abre a janela dele.

1. Traduza o teste com o vmtranslator. O `./scripts/validate.sh` traduz os 11 de uma vez em `output/`.
2. No CPU Emulator, abra o `.tst` do teste pelo menu *File*, opção *Load Script*. O script carrega o `.asm` da mesma pasta.
3. Escolha *No animation* no controle de animação e clique em *Run*.

Ao fim, a barra de status mostra `End of script - Comparison ended successfully`. O script executa o `.asm` por um número fixo de ciclos, grava algumas posições da RAM num arquivo `.out` e compara esse arquivo com o `.cmp` que acompanha o teste.

O mesmo script roda sem janela quando recebe o `.tst` como argumento, e é assim que o `validate.sh` usa o emulador.

```bash
sh tools/CPUEmulator.sh output/8/FunctionCalls/FibonacciElement/FibonacciElement.tst
```

## Funcionalidades

- Comandos aritméticos e lógicos `add`, `sub`, `neg`, `eq`, `gt`, `lt`, `and`, `or` e `not`
- `push` nos oito segmentos da VM e `pop` nos sete que aceitam escrita
- Desvios `label`, `goto` e `if-goto`, com rótulos qualificados pela função corrente
- Funções com `function`, `call` e `return`, seguindo o protocolo de chamada do capítulo 13 do tutorial
- Tradução de um diretório inteiro para um único `.asm`
- Código de inicialização gerado quando o diretório tem um `Sys.vm`
- O comando VM como comentário antes do Assembly de cada tradução
- Mensagens de erro para comando desconhecido, número errado de argumentos, índice inválido, segmento desconhecido, `pop constant` e índices fora do limite em `constant`, `temp` e `pointer`

## Testes e validação

O Maven roda os testes automatizados com JUnit 5.

```bash
./mvnw test
```

O resumo do Maven mostra os 39 testes passando. São 10 do `Parser`, 23 do `CodeWriter` e 6 do `Main`.

```
[INFO] Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

O script `validate.sh` compila o jar e roda os 11 testes oficiais. Ele copia cada teste para `output/`, traduz a cópia e passa o `.tst` para o CPU Emulator da pasta `tools/`. O emulador termina com código 0 quando a comparação bate, e o script mostra uma linha OK ou FAIL por teste. Os arquivos de `src/test/resources` não mudam.

```bash
./scripts/validate.sh
```

```
OK   7/StackArithmetic/SimpleAdd
OK   7/StackArithmetic/StackTest
OK   7/MemoryAccess/BasicTest
OK   7/MemoryAccess/PointerTest
OK   7/MemoryAccess/StaticTest
OK   8/ProgramFlow/BasicLoop
OK   8/ProgramFlow/FibonacciSeries
OK   8/FunctionCalls/SimpleFunction
OK   8/FunctionCalls/NestedCall
OK   8/FunctionCalls/FibonacciElement
OK   8/FunctionCalls/StaticsTest
11/11 OK
```

| Teste | Entrada | O que exercita | Resultado |
| --- | --- | --- | --- |
| `StackArithmetic/SimpleAdd` | arquivo | `push constant` e `add` | passou |
| `StackArithmetic/StackTest` | arquivo | Todos os comandos aritméticos e lógicos, com as comparações | passou |
| `MemoryAccess/BasicTest` | arquivo | `push` e `pop` em `constant`, `local`, `argument`, `this`, `that` e `temp` | passou |
| `MemoryAccess/PointerTest` | arquivo | `pointer`, `this` e `that` | passou |
| `MemoryAccess/StaticTest` | arquivo | `static` | passou |
| `ProgramFlow/BasicLoop` | arquivo | `label` e `if-goto` num laço que soma de 1 a n | passou |
| `ProgramFlow/FibonacciSeries` | arquivo | `goto` e `if-goto` num laço que grava a série de Fibonacci na memória | passou |
| `FunctionCalls/SimpleFunction` | arquivo | `function` e `return` com variáveis locais | passou |
| `FunctionCalls/NestedCall` | diretório | Chamadas aninhadas, variáveis locais zeradas e o frame preservado | passou |
| `FunctionCalls/FibonacciElement` | diretório | Recursão e dois arquivos num só `.asm`, com o código de inicialização | passou |
| `FunctionCalls/StaticsTest` | diretório | `static` separado por arquivo, com o código de inicialização | passou |

A tag `parte1` marca o commit em que os cinco testes do Project 7 passaram pela primeira vez.

## Estrutura do repositório

| Pasta | Conteúdo |
| --- | --- |
| `src/main/java/` | Os pacotes `parser`, `codewriter` e `main` |
| `src/test/java/` | Os testes JUnit, no mesmo pacote de cada classe |
| `src/test/resources/projects/7/` | Os cinco testes oficiais do Project 7, um por pasta |
| `src/test/resources/projects/8/` | Os seis testes oficiais do Project 8, um por pasta |
| `scripts/` | O `validate.sh` |
| `tools/` | O CPU Emulator do pacote de software do Nand2Tetris, com o `NOTICE.md` |
| `docs/` | A documentação detalhada |
| `output/` | As cópias dos testes e os `.asm` gerados pelo `validate.sh`, fora do Git |

O pacote `main` depende dos outros dois, e `parser` e `codewriter` não dependem um do outro. Cada pasta de teste traz o `.vm` de entrada, o `.tst` que o CPU Emulator executa e o `.cmp` com o resultado esperado. O `XxxVME.tst` de cada teste, que serve ao VM Emulator, e as páginas HTML do NestedCall ficaram fora, porque nenhuma parte do repositório usa esses arquivos.

## Documentação detalhada

- [Parser](docs/parser.md)
- [CodeWriter](docs/codewriter.md)

Os documentos descrevem o funcionamento de cada módulo, o Assembly gerado para cada comando, os testes e as diferenças em relação ao tutorial.

## Decisões de projeto

O projeto usa Maven com JUnit 5, que o tutorial não usa. Os testes oficiais ficam dentro do repositório, junto com o CPU Emulator, para que a validação rode só com o Java instalado.

O `CodeWriter` tem `writePush` e `writePop` separados, e não um único `writePushPop` como no capítulo 12 do tutorial. Os tipos de comando são categorias, como `C_ARITHMETIC`, `C_PUSH` e `C_POP`, e não um valor por comando como na implementação da LambdaGeo.

Cada comparação usa um par de rótulos com um contador do programa inteiro, como `TRUE_EQ_0` e `END_EQ_0`. Os rótulos de `label`, `goto` e `if-goto` seguem o formato `função$rótulo`. Nos arquivos sem nenhum `function`, como o `BasicLoop.vm`, o prefixo é o nome do arquivo, e o laço vira `BasicLoop$LOOP`.

O rótulo de retorno de cada `call` tem o formato `função$ret.i`. O capítulo 13 sugere um contador por função, e o tradutor usa um contador do programa inteiro. Com isso a unicidade do rótulo não depende do nome da função, e o código de inicialização, que não fica dentro de nenhuma função, usa o rótulo `Bootstrap$ret.0`.

O `function` repete o código que empilha zero, uma vez por variável local, sem laço em Assembly. O capítulo 13 recomenda essa forma, porque um laço precisaria de rótulos internos que poderiam coincidir com rótulos do programa.

A pasta `tools/` traz só os arquivos que o CPU Emulator abre, levantados ao rodar o emulador em modo de teste e com a janela aberta. O arquivo `tools/bin/CPU Emulator.dat` fica fora do Git. O emulador grava nele a última pasta usada a cada execução e o recria quando falta, e no repositório ele sujaria o `git status` depois de cada validação.

## Limitações conhecidas

- As comparações `eq`, `gt` e `lt` calculam `x - y` e testam o sinal do resultado. Quando `x` e `y` têm sinais opostos e a diferença passa de 32767, a subtração estoura os 16 bits e a resposta sai errada. Com `x = 20000` e `y = -20000`, `gt` responde falso. Os testes oficiais não exercitam esse caso.
- O tradutor não confere os nomes de rótulos e funções. Um nome que o Assembly Hack não aceita, como um que comece com dígito, passa pela tradução e só falha no Assembler ou no CPU Emulator.
- O tradutor não limita o índice de `static`. O Assembler aloca essas variáveis a partir de `RAM[16]`, e um programa com mais de 240 variáveis `static` invadiria a área da pilha, em `RAM[256]`.
- O código de inicialização sai quando o diretório tem um arquivo chamado `Sys.vm`. O tradutor não confere se esse arquivo declara a função `Sys.init`. Sem ela, o Assembler trata `Sys.init` como uma variável, e o salto do código de inicialização vai para um endereço da ROM sem relação com o programa.

## Desafios e aprendizados

O livro traz a teoria, o pseudocódigo e o Assembly de cada comando, mas a dupla precisou entender como esse material vira um tradutor. A maior parte do trabalho foi essa leitura, e a IA explicou os trechos que não ficavam claros.

O frame de chamada pediu mais tempo. O `call` empilha o endereço de retorno e os registradores `LCL`, `ARG`, `THIS` e `THAT` de quem chama, reposiciona `ARG` e `LCL` e salta para a função. O `return` desfaz esse frame na ordem inversa, e a dupla precisou entender por que ele lê o endereço de retorno antes de gravar o valor de retorno. Numa função chamada sem argumentos, `ARG` aponta para a mesma posição do endereço de retorno.

O código de inicialização também ficou claro só ao acompanhar os números. Ele põe `SP` em 256 e faz um `call Sys.init 0` completo. Os cinco valores empilhados levam `SP` e `LCL` para 261, e `ARG` fica em 256. O FibonacciElement e o StaticsTest conferem esses valores. Antes do commit do código de inicialização, o FibonacciElement falhava no `validate.sh` com um acesso a endereço negativo, porque o `.tst` dele não inicializa o `SP`.

As comparações mostraram como uma máquina sem instrução de comparar produz um booleano. O tradutor subtrai, salta conforme o sinal e grava -1 ou 0, e cada comparação precisa dos próprios rótulos.

A dupla saiu do projeto entendendo como uma pilha e os segmentos da VM se reduzem a dois registradores e uma RAM. Ficou mais fácil ler o que um compilador gera e enxergar o custo de cada chamada de função.

## Uso de inteligência artificial

A dupla usou o Claude, assistente de IA da Anthropic, pelo site claude.ai com o recurso Projetos. O modelo foi o Claude Opus 5.5, com esforço médio.

O livro da disciplina traz a teoria, o pseudocódigo e o Assembly de cada comando, mas não traz o código do tradutor passo a passo. A IA ajudou nas tarefas abaixo.

- Criar o tutorial a partir do livro da disciplina, com um passo a passo explicado para seguir na implementação.
- Sugerir a divisão do trabalho entre os integrantes e a sequência de commits, com as mensagens de cada um.
- Tirar dúvidas e aprofundar partes do livro, como os segmentos da VM e o protocolo de chamada de funções.
- Escrever testes unitários com JUnit.
- Escrever e revisar a documentação, incluindo este README.

Tudo o que a IA sugeriu passou pelos testes descritos em [Testes e validação](#testes-e-validação) antes de entrar no repositório.

## Vídeo de demonstração

[Vídeo de demonstração](LINK_DO_VIDEO)

## Licença

O código do projeto está sob a licença MIT, conforme o arquivo [LICENSE](LICENSE). A pasta `tools/` fica fora dessa licença. A origem das ferramentas e as declarações de licença do Nand2Tetris estão em [tools/NOTICE.md](tools/NOTICE.md).