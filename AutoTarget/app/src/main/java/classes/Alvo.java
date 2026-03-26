package classes;

public abstract class Alvo extends Thread{
    protected double x, y;
    protected double raio = 25;
    protected double velocidade;
    protected double dx = 1;
    protected double dy = 1;
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
        // Se bater na borda esquerda ou direita, inverte o direção X
        if (x - raio < 0) {
            x = raio;
            dx = dx * (-1);
        } else if (x + raio > jogo.getLarguraTela()) {
            x = jogo.getLarguraTela() - raio;
            dx = dx * (-1);
        }

        // Se bater no teto ou chão, inverte a direção Y
        if (y - raio < 0) {
            y = raio;
            dy = dy * (-1);
        } else if (y + raio > jogo.getAlturaTela()) {
            y = jogo.getAlturaTela() - raio;
            dy = dy * (-1);
        }
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
