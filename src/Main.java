import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Uso:
 *   java Main                      -> roda todos os testes nos textos de Amostras/ e gera resultados.csv
 *   java Main <arquivo.txt> <palavra> -> roda cada método uma vez para o arquivo e a palavra informados
 */
public class Main {

    static final String PASTA_AMOSTRAS = "Amostras";
    static final String ARQUIVO_CSV = "resultados.csv";
    static final String PALAVRA_PADRAO = "the";
    static final int EXECUCOES = 3;

    static final int NUCLEOS = Runtime.getRuntime().availableProcessors();
    static final int[] THREADS = IntStream.of(1, 2, 4, 8, NUCLEOS).distinct().sorted().toArray();

    interface Contagem {
        int executar() throws Exception;
    }

    // contagem do SerialCPU, usada para conferir se os outros métodos dão o mesmo número
    static int referencia;

    public static void main(String[] args) throws Exception {
        if (args.length == 2) {
            executarUmaVez(Path.of(args[0]), args[1]);
        } else {
            executarTestes();
        }
    }

    /** Lê o arquivo e deixa o texto no formato combinado: minúsculas e pontuação trocada por espaço. */
    static String prepararTexto(Path arquivo) throws Exception {
        String texto = Files.readString(arquivo).toLowerCase();
        return texto.replaceAll("[^\\p{L}\\p{N}]+", " ");
    }

    static void executarUmaVez(Path arquivo, String palavra) throws Exception {
        String texto = prepararTexto(arquivo);
        String p = palavra.toLowerCase();

        rodar("SerialCPU", null, "1", 1, () -> SerialCPU.contar(texto, p), null);
        rodar("ParallelCPU", null, "" + NUCLEOS, 1, () -> ParallelCPU.contar(texto, p, NUCLEOS), null);
        rodar("ParallelGPU", null, "-", 1, () -> ParallelGPU.contar(texto, p), null);
    }

    static void executarTestes() throws Exception {
        List<Path> arquivos;
        try (var lista = Files.list(Path.of(PASTA_AMOSTRAS))) {
            arquivos = lista.filter(f -> f.toString().endsWith(".txt"))
                            .sorted(Comparator.comparingLong(f -> f.toFile().length()))
                            .toList();
        }

        try (PrintWriter csv = new PrintWriter(ARQUIVO_CSV, "UTF-8")) {
            csv.println("metodo,arquivo,threads,execucao,ocorrencias,tempo_ms");

            for (Path arquivo : arquivos) {
                String nome = arquivo.getFileName().toString().replace(".txt", "");
                String texto = prepararTexto(arquivo);
                String p = PALAVRA_PADRAO;

                System.out.println("\n=== " + nome + " | palavra: \"" + p + "\" ===");

                rodar("SerialCPU", nome, "1", EXECUCOES, () -> SerialCPU.contar(texto, p), csv);

                for (int t : THREADS) {
                    rodar("ParallelCPU", nome, "" + t, EXECUCOES, () -> ParallelCPU.contar(texto, p, t), csv);
                }

                rodar("ParallelGPU", nome, "-", EXECUCOES, () -> ParallelGPU.contar(texto, p), csv);
            }
        }

        System.out.println("\nResultados salvos em " + ARQUIVO_CSV);
    }

    /**
     * Roda um método 'execucoes' vezes, imprime cada resultado e grava no CSV (se csv != null).
     * Antes, faz uma execução de aquecimento que não é registrada (o Java fica mais rápido
     * depois que o JIT compila o código, e isso distorceria a primeira medida).
     */
    static void rodar(String metodo, String arquivo, String threads, int execucoes,
                      Contagem contagem, PrintWriter csv) throws Exception {
        String rotulo = metodo.equals("ParallelCPU") ? metodo + " (" + threads + " threads)" : metodo;

        try {
            contagem.executar(); // aquecimento
        } catch (UnsupportedOperationException e) {
            System.out.println(rotulo + ": " + e.getMessage());
            return;
        }

        for (int i = 1; i <= execucoes; i++) {
            long inicio = System.nanoTime();
            int ocorrencias = contagem.executar();
            double ms = (System.nanoTime() - inicio) / 1_000_000.0;

            System.out.printf("%s: %d ocorrências em %.2f ms%n", rotulo, ocorrencias, ms);

            if (metodo.equals("SerialCPU")) {
                referencia = ocorrencias;
            } else if (ocorrencias != referencia) {
                System.out.println("  AVISO: contagem diferente do SerialCPU (" + referencia + ")");
            }

            if (csv != null) {
                csv.printf(Locale.US, "%s,%s,%s,%d,%d,%.3f%n",
                           metodo, arquivo, threads, i, ocorrencias, ms);
            }
        }
    }
}
