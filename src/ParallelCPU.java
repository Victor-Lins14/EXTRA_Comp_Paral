import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Versão paralela na CPU: divide o texto em N partes (uma por thread)
 * e cada thread conta as ocorrências na sua parte usando o mesmo
 * algoritmo da versão serial. No final, soma os resultados.
 */
public class ParallelCPU {

    public static int contar(String texto, String palavra, int numThreads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        List<Future<Integer>> resultados = new ArrayList<>();

        int tamanhoParte = texto.length() / numThreads;
        int inicio = 0;

        for (int t = 0; t < numThreads; t++) {
            int fim;
            if (t == numThreads - 1) {
                fim = texto.length();
            } else {
                // avança o corte até o próximo espaço para não partir uma palavra ao meio
                fim = texto.indexOf(' ', Math.max(inicio, (t + 1) * tamanhoParte));
                if (fim == -1) fim = texto.length();
            }

            String parte = texto.substring(inicio, fim);
            resultados.add(pool.submit(() -> SerialCPU.contar(parte, palavra)));
            inicio = fim;
        }

        int total = 0;
        for (Future<Integer> r : resultados) {
            total += r.get();
        }

        pool.shutdown();
        return total;
    }
}
