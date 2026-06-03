package classes;
import android.graphics.Color;

public class AlvoComum extends Alvo{
    public AlvoComum(double x, double y, Jogo jogo){
        super(x, y, jogo);
        this.velocidade = 5.0;
    }
    
    @Override
    public void mover(){
        x += velocidade * dx;
        y += velocidade * 0.5 * dy;
    }

    @Override
    public int getCor() {
        return Color.BLUE;
    }
}
