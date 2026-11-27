/**
 * Versão serial: percorre cada palavra do texto com um loop simples
 * e conta quantas são iguais à palavra buscada.
 */
public class SerialCPU {

    public static int contar(String texto, String palavra) {
        int total = 0;
        for (String p : texto.split(" ")) {
            if (p.equals(palavra)) {
                total++;
            }
        }
        return total;
    }
}
