package classes;

public class AlvoRapido extends Alvo{
    public AlvoRapido(double x, double y, Jogo jogo){
        super(x, y, jogo);
        this.velocidade = 12.0;
    }
    @Override
    public void mover(){
        // Movimento errático e rápido
        x -= velocidade * dx;
        y += velocidade * 0.5 * dy; 
    }
}
