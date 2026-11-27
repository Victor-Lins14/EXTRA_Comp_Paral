# Plano de Implementação — Contagem de palavras: Serial × Paralelo CPU × Paralelo GPU

Trabalho em dupla, dividido em duas partes. A ideia é manter tudo **simples**: poucos arquivos, sem pacotes e sem frameworks.

| Gabriel — **CPU + testes** | Victor — **GPU + gráficos + relatório** |
|---|---|
| `SerialCPU`, `ParallelCPU`, `Main`, geração do CSV | `ParallelGPU` (JOCL), gráficos, README e PDF |

---

## Combinado entre os dois (antes de começar)

**1. Como preparar o texto.** Os três métodos usam o mesmo texto já tratado, assim todos dão a mesma contagem:

```java
String texto = Files.readString(Path.of(arquivo)).toLowerCase();
texto = texto.replaceAll("[^\\p{L}\\p{N}]+", " ");   // tudo que não é letra/número vira espaço
```

**2. Assinatura dos métodos.** Cada método recebe o texto já tratado e a palavra, e devolve a contagem:

```java
static int contar(String texto, String palavra)
```

**3. O que entra no tempo.** Só a chamada de `contar`. A leitura do arquivo fica fora da medição.

**Estrutura do projeto:**
```
src/
  Main.java          ← Gabriel
  SerialCPU.java     ← Gabriel
  ParallelCPU.java   ← Gabriel
  ParallelGPU.java   ← Victor
resultados.csv       ← gerado pelo Main
graficos.ipynb       ← Victor
img/                 ← PNGs dos gráficos
README.md            ← relatório (ambos)
```

---

## Parte 1 — Gabriel: CPU e testes

### `SerialCPU.java`
Um loop simples sobre as palavras:
```java
static int contar(String texto, String palavra) {
    int total = 0;
    for (String p : texto.split(" "))
        if (p.equals(palavra)) total++;
    return total;
}
```

### `ParallelCPU.java`
- Quebrar o texto em palavras (`split(" ")`).
- Dividir o array de palavras em `N` partes, uma por thread.
- Usar `Executors.newFixedThreadPool(N)`: cada tarefa conta a sua parte com o mesmo loop do serial.
- Somar os resultados dos `Future`s e chamar `shutdown()`.
- Receber o número de threads como parâmetro, para variar 1, 2, 4 e 8 (objetivo 3 do enunciado).

### `Main.java`
Para cada arquivo de `Amostras/`, cada método e cada configuração:
1. Rodar **3 vezes** (o mínimo do enunciado) e medir com `System.nanoTime()`.
2. Imprimir no formato pedido:
   ```
   SerialCPU: 66 ocorrências em 133 ms
   ParallelCPU: 66 ocorrências em 82 ms
   ParallelGPU: 66 ocorrências em 95 ms
   ```
3. Gravar uma linha no `resultados.csv`:
   ```
   metodo,arquivo,threads,execucao,ocorrencias,tempo_ms
   SerialCPU,Dracula,1,1,1234,12.5
   ParallelCPU,Dracula,4,1,1234,5.1
   ParallelGPU,Dracula,-,1,1234,30.2
   ```

Os três textos de amostra já têm tamanhos diferentes (cerca de 165 mil, 217 mil e 388 mil palavras), o que atende ao pedido de variar o tamanho da entrada.

**Palavra de teste:** usar uma palavra comum, como `the`, em todos os testes.

### Entregáveis do Gabriel
- [ ] `SerialCPU`, `ParallelCPU` e `Main` funcionando.
- [ ] `resultados.csv` gerado.
- [ ] Texto do README sobre a parte de CPU (como foi feita e o que os resultados mostram).

---

## Parte 2 — Victor: GPU, gráficos e relatório

### `ParallelGPU.java` (com JOCL)
Ideia: transformar o texto em bytes e criar uma thread da GPU para cada posição do texto. Cada uma verifica se a palavra começa ali.

Kernel (fica como `String` dentro do próprio `.java`):
```c
__kernel void contar(__global const char* texto, int n,
                     __global const char* palavra, int m,
                     __global int* total) {
    int i = get_global_id(0);
    if (i + m > n) return;
    if (i > 0 && texto[i-1] != ' ') return;      // tem que começar uma palavra
    if (i + m < n && texto[i+m] != ' ') return;  // tem que terminar a palavra
    for (int k = 0; k < m; k++)
        if (texto[i+k] != palavra[k]) return;
    atomic_inc(total);
}
```

Passos no Java, seguindo os exemplos do site do JOCL:
1. Pegar a plataforma e a GPU, e criar o contexto e a fila (`clCreateContext`, `clCreateCommandQueue`).
2. Compilar o kernel (`clCreateProgramWithSource`, `clBuildProgram`, `clCreateKernel`).
3. Criar os buffers: texto, palavra e `total` (começando em 0).
4. Executar com `clEnqueueNDRangeKernel`, usando `n` threads.
5. Ler `total` com `clEnqueueReadBuffer` e liberar os recursos.

> Os bytes de letras acentuadas nunca são o byte do espaço, então a verificação de fronteira continua funcionando.

### Gráficos (`graficos.ipynb`, com pandas + matplotlib)
Ler o `resultados.csv`, tirar a média das 3 execuções e gerar:
1. **Tempo por arquivo**: barras para Serial, ParallelCPU e GPU.
2. **Tempo da ParallelCPU × número de threads.**
3. (Opcional) **Speedup**, calculado como `tempo_serial / tempo_paralelo`.

Salvar como PNG em `img/` para usar no README.

### README.md (relatório)
Seções pedidas pelo enunciado:
- **Resumo**
- **Introdução**: os 3 métodos.
- **Metodologia**: como foi medido, quantas execuções e a máquina usada (CPU, núcleos e GPU).
- **Resultados e Discussão**: os gráficos e o que eles mostram.
- **Conclusão**
- **Referências**
- **Anexos**: os códigos e, no final, o link do GitHub.

Incluir também a seção **"Como executar"**, que o enunciado pede porque o projeto usa uma biblioteca:
```
javac -cp jocl-2.0.4/jocl-2.0.4.jar -d out src/*.java
java -cp "out;jocl-2.0.4/jocl-2.0.4.jar" Main
```
Explicar que o `jocl-2.0.4.jar` já está na pasta `jocl-2.0.4/` do repositório e que é preciso ter o driver da placa de vídeo instalado, com suporte a OpenCL. No Linux e no macOS, trocar o `;` por `:`.

### Entregáveis do Victor
- [ ] `ParallelGPU` dando a mesma contagem do serial.
- [ ] Gráficos em `img/`.
- [ ] README completo.
- [ ] PDF do README.

---

## Ordem de trabalho

1. **Juntos:** combinar o tratamento do texto e a assinatura `contar(texto, palavra)`.
2. **Separados:** cada um faz a sua parte.
3. **Juntos:** colocar a GPU no `Main` e conferir que os 3 métodos dão o mesmo número.
4. **Gabriel:** gerar o `resultados.csv`.
5. **Victor:** fazer os gráficos e montar o README.
6. **Juntos:** revisar, gerar o PDF e enviar com o link do GitHub.
