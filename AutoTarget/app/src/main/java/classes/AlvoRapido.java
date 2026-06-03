package classes;
import android.graphics.Color;

public class AlvoRapido extends Alvo{
    private double tempoDeVida = 0;
    
    public AlvoRapido(double x, double y, Jogo jogo){
        super(x, y, jogo);
        this.velocidade = 8.0;
    }
    
    @Override
    public void mover(){
        tempoDeVida += 0.1;
        // Polimorfismo substantivo (AV1 fix): Movimento senoidal em Y (zigue-zague)
        x += velocidade * dx;
        y += (Math.sin(tempoDeVida) * 10.0) * dy;
    }

    @Override
    public int getCor() {
        return Color.RED;
    }
}
