package classes;

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
            Alvo alvoMaisProximo = jogo.getAlvoMaisProximo(x, y, isLadoEsquerdo);

            if (alvoMaisProximo != null){
                Projetil p = new Projetil(this.x, this.y, alvoMaisProximo.getX(), alvoMaisProximo.getY(), jogo, isLadoEsquerdo);
                jogo.adicionarProjetil(p);
                p.start();
            }

            // Cálculo da penalidade de tempo de recarga
            int qtdCanhoesLado = jogo.getQtdCanhoes(isLadoEsquerdo);
            long tempoRecarga = 1000; // 1 segundo base
            if (qtdCanhoesLado > 5) {
                tempoRecarga += (qtdCanhoesLado - 5) * 200; // +200ms por canhão extra
            }

            try{
                Thread.sleep(tempoRecarga);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
    }

    public void desligar(){
        this.ativo = false;
    }
    public boolean isLadoEsquerdo(){
        return isLadoEsquerdo;
    }
    public double getX() { return x; }
    public double getY() { return y; }
}
