package classes;

public class Canhao extends Thread{
    private double x, y;
    private double targetX, targetY; // Para movimento suave
    private volatile boolean ativo = true;
    private Jogo jogo;
    private boolean isLadoEsquerdo;

    public Canhao(double x, double y, Jogo jogo) throws JogoException{
        if ( x < 0 || y < 0 || x > jogo.getLarguraTela() || y > jogo.getAlturaTela() ){
            throw new JogoException("Posição do canhão fora dos limites da tela.");
        }
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.jogo = jogo;
        this.isLadoEsquerdo = (x < jogo.getLarguraTela() / 2.0);
    }

    // Permite que o canhão seja realocado pelo otimizador de forma suave
    public void setNovaPosicao(double novoX, double novoY) {
        this.targetX = novoX;
        this.targetY = novoY;
    }

    private void moverSuavemente() {
        // Canhões movem SOMENTE na horizontal (eixo X)
        if (Math.abs(this.x - this.targetX) > 2.0) {
            this.x += (this.targetX - this.x) * 0.1;
        }
        // Y é fixo: ignoramos targetY para evitar drift vertical
    }

    private long ultimoTiro = 0;

    @Override
    public void run(){
        while (ativo){
            moverSuavemente();
            
            long agora = System.currentTimeMillis();
            boolean temEnergia = isLadoEsquerdo() ? (jogo.getEnergiaEsquerda() > 0) : (jogo.getEnergiaDireita() > 0);

            // Cálculo da penalidade de tempo de recarga
            int qtdCanhoesLado = jogo.getQtdCanhoes(isLadoEsquerdo);
            long tempoRecarga = 1000;
            if (qtdCanhoesLado > 5) {
                double fatorPenalidade = 1.0 + ((qtdCanhoesLado - 5) * 0.2);
                tempoRecarga = (long) (1000 * fatorPenalidade);
            }

            if (temEnergia && (agora - ultimoTiro > tempoRecarga)) {
                Alvo alvoMaisProximo = jogo.getAlvoMaisProximo(x, y, isLadoEsquerdo);

                if (alvoMaisProximo != null){
                    Projetil p = new Projetil(this.x, this.y, alvoMaisProximo.getX(), alvoMaisProximo.getY(), jogo, isLadoEsquerdo);
                    jogo.adicionarProjetil(p);
                    p.start();
                    ultimoTiro = agora;
                }
            }

            try{
                Thread.sleep(16); // 60fps movement tick
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void desligar(){ this.ativo = false; }
    public boolean isLadoEsquerdo(){ return isLadoEsquerdo; }
    public double getX() { return x; }
    public double getY() { return y; }
}
