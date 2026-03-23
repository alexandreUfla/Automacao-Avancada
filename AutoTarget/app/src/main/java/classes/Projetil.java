package classes;
import classes.Jogo;
import classes.Alvo;
import classes.Canhao;

public class Projetil extends Thread{
    private double x, y;
    private double dirX, dirY;
    private double velocidade = 20.0;
    private volatile boolean ativo = true;
    private Jogo jogo;
    private boolean isLadoEsquerdo; // Para pontuação

    public Projetil(double startX, double startY, double alvoX, double alvoY, Jogo jogo, boolean isLadoEsquerdo){
        this.x = startX;
        this.y = startY;
        this.jogo = jogo;
        this.isLadoEsquerdo = isLadoEsquerdo;

        // Bloco try-catch exigido para evitar divisão por zero
        try{
            double dx = alvoX - startX;
            double dy = alvoY - startY;
            double distancia = Math.sqrt(dx * dx + dy * dy);
            if(distancia == 0) throw new ArithmeticException("Distância zero ao calcular direção");

            this.dirX = dx / distancia;
            this.dirY = dy / distancia;
        } catch (ArithmeticException e){
            // Direção padrão em caso de erro
            this.dirX = 1;
            this.dirY = 0;
        }
    }

    @Override
    public void run(){
        while(ativo){
            x += dirX * velocidade;
            y += dirY * velocidade;

            // Verifica colisão ou se saiu da tela
            jogo.verificarColisao(this);
            if(x < 0 || x > jogo.getLarguraTela() || y < 0 || y > jogo.getAlturaTela()){
                ativo = false;
                jogo.removerProjetil(this);
            }

            try{
                Thread.sleep(16);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
    }

    public void destruir(){ this.ativo = false; }
    public double getX(){ return x; }
    public double getY(){ return y; }
    public boolean isLadoEsquerdo(){ return isLadoEsquerdo; }
}
