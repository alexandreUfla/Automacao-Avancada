package classes;
import java.util.Random;

public class SensorVirtual {
    private static final Random random = new Random();
    private static final double RUIDO_PERCENTUAL = 0.05; // 5% de ruído

    // Retorna um valor com ruído gaussiano adicionado
    public static double lerComRuido(double valorReal) {
        double desvioPadrao = Math.abs(valorReal) * RUIDO_PERCENTUAL;
        if (desvioPadrao == 0) desvioPadrao = 0.1; // Evita DP zero
        double ruido = random.nextGaussian() * desvioPadrao;
        return valorReal + ruido;
    }

    public static class Leitura {
        public double x, y, vx, vy;
        public long timestamp;

        public Leitura(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
