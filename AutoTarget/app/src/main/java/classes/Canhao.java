package classes;
import classes.Jogo;

public class Canhao extends Thread{
    private double x, y;
    private volatile boolean ativo = true;
    private Jogo jogo;
    private boolean isLadoEsquerdo;

    public Canhao(double x, double y, Jogo jogo) throws JogoException{
        if ( x < 0 || y < 0 || x > jogo.getLarguraTela() || y > jogo.getAlturaTela() ){
            throw new JogoException("Posição do canhão fora dos limites da tela.");
        }
        this.x = x;
        this.y = y;
        this.jogo = jogo;
        this.isLadoEsquerdo = (x < jogo.getLarguraTela() / 2.0);
    }

    @Override
    public void run(){
        while (ativo){

        }
    }
}
