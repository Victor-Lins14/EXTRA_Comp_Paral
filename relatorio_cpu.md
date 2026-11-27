# Texto da parte de CPU (Integrante 1), para colar no README

> Os números abaixo vêm de uma execução preliminar, feita só com a CPU.
> Depois que a GPU estiver pronta, rode `java Main` de novo e atualize a tabela com o `resultados.csv` final.

## Introdução: métodos de CPU

- **SerialCPU:** o texto é dividido em palavras (`split(" ")`) e um loop simples compara cada palavra com a palavra buscada, somando 1 a cada ocorrência.
- **ParallelCPU:** o texto é dividido em N partes, uma por thread. Cada corte é empurrado até o próximo espaço, para não partir uma palavra ao meio. As partes são entregues a um pool de threads (`Executors.newFixedThreadPool(N)`). Cada thread conta as ocorrências da sua parte com o mesmo algoritmo do SerialCPU, e no final os resultados parciais (`Future`) são somados.

## Metodologia (parte de CPU)

- **Preparação do texto:** antes da contagem, o texto é lido em UTF-8, convertido para minúsculas, e tudo que não é letra nem número vira espaço. Os três métodos usam esse mesmo texto, por isso as contagens são iguais. A leitura e a preparação ficam fora da medição de tempo.
- **Medição:** com `System.nanoTime()`, só durante a chamada de contagem. No caso da ParallelCPU, a medição inclui a criação do pool de threads.
- **Execuções:** cada configuração roda **1 vez como aquecimento**, que não é registrada (dá tempo para o JIT do Java compilar o código), e depois **3 vezes medidas**. O resultado de cada uma vai para o `resultados.csv`.
- **Variação do paralelismo:** a ParallelCPU foi testada com 1, 2, 4, 8 e 12 threads. Doze é o número de processadores lógicos da máquina.
- **Entradas:** os três textos de amostra (Dracula, Moby Dick e Dom Quixote), com cerca de 165 mil, 217 mil e 388 mil palavras. A palavra buscada foi `the`.
- **Conferência:** toda contagem paralela é comparada com a do SerialCPU. As contagens também foram conferidas com um script Python independente.
- **Máquina:** Intel Core i5-13420H (8 núcleos: 4 de desempenho com Hyper-Threading e 4 de eficiência, totalizando 12 threads), Java 23 (OpenJDK 23.0.1), Windows 11.

## Resultados e Discussão (parte de CPU)

Tempo médio de 3 execuções, em ms. Speedup = tempo do SerialCPU ÷ tempo da ParallelCPU.

| Arquivo | Serial | 1 thread | 2 threads | 4 threads | 8 threads | 12 threads |
|---|---|---|---|---|---|---|
| Dracula (165 mil palavras) | 6,43 | 9,95 | 2,80 (2,3×) | 1,93 (3,3×) | 1,97 (3,3×) | 2,38 (2,7×) |
| Moby Dick (217 mil palavras) | 9,04 | 9,52 | 4,03 (2,2×) | 3,14 (2,9×) | 1,97 (4,6×) | 1,84 (4,9×) |
| Dom Quixote (388 mil palavras) | 17,94 | 11,68 | 8,19 (2,2×) | 7,06 (2,5×) | 3,01 (6,0×) | 3,81 (4,7×) |

O que os dados mostram:

- **A versão paralela é mais rápida a partir de 2 threads**, e o ganho cresce até mais ou menos 8 threads. O maior speedup foi de cerca de 6× no Dom Quixote, com 8 threads.
- **Quanto maior o texto, maior o ganho.** No Dracula, o menor arquivo, o speedup já para em 4 threads: há pouco trabalho para dividir, e o custo de criar e coordenar as threads pesa mais. No Dom Quixote, o maior, o ganho continua até 8 threads.
- **Com 12 threads o ganho cai ou não melhora** em relação a 8. O processador tem 8 núcleos físicos, e 4 deles são núcleos de eficiência, mais lentos. Passar desse ponto adiciona threads que disputam os mesmos núcleos.
- **Com 1 thread, a ParallelCPU fica parecida com a serial.** Ela faz o mesmo trabalho, mais o custo do pool. As diferenças vêm do ruído da medição.
- **Os tempos são pequenos (alguns milissegundos)**, então variam bastante de uma execução para outra. Por exemplo, uma das medidas do Dom Quixote no serial deu 29 ms, enquanto as outras deram cerca de 12 ms. Isso também explica speedups acima do número de threads, como 2,3× com 2 threads. Por isso usamos a média de 3 execuções.
- **Dom Quixote está em espanhol**, então a palavra `the` aparece só 188 vezes, contra 8.104 no Dracula e 14.727 no Moby Dick. Isso não afeta o tempo da CPU, porque o algoritmo compara todas as palavras de qualquer forma.
