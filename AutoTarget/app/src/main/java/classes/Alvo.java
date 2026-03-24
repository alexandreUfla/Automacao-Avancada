package classes;

public abstract class Alvo extends Thread{
    protected double x, y;
    protected double raio = 10;
    protected double velocidade;
    protected volatile boolean ativo = true;
    protected Jogo jogo;

    public Alvo(double x, double y, Jogo jogo){
        this.x = x;
        this.y = y;
        this.jogo = jogo;
    }

    // Método que será sobrescrito demostrando o Polimorfismo
    public abstract void mover();

    @Override
    public void run(){
        while(ativo){
            mover();
            /*
            Limita a tela para que o alvo não fuja e troque
            de lado simulando o cenário
             */
            limitesTela();
            try{
                // ~30 frames por segundo para a atualização de física
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void limitesTela(){
        if (x < 0) x = jogo.getLarguraTela();
        if (x > jogo.getLarguraTela()) x = 0;
        if (y < 0) y = jogo.getAlturaTela();
        if (y > jogo.getAlturaTela()) y = 0;
    }

    public void destruir(){
        this.ativo = false;
    }
    public double getX(){
        return x;
    }
    public double getY(){
        return y;
    }
    public double getRaio(){
        return raio;
    }
    public boolean isAtivo(){ return ativo; }
}
