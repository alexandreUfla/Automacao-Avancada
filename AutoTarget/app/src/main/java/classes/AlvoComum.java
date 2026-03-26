package classes;

public class AlvoComum extends Alvo{
    public AlvoComum(double x, double y, Jogo jogo){
        super(x, y, jogo);
        this.velocidade = 5.0;
    }
    @Override
    public void mover(){
        // Movimento simples e linear
        x += velocidade * dx;
        y += velocidade * 0.5 * dy;
    }
}
