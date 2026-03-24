package classes;
import java.util.ArrayList;
import java.util.List;

public class Jogo extends Thread{
    private List<Alvo> alvos = new ArrayList<>();
    private List<Canhao> canhoes = new ArrayList<>();
    private List<Projetil> projeteis = new ArrayList<>();

    // Objetos para lock (Regiões críticas)
    private final Object lockListas = new Object();
    private final Object lockColisao = new Object();

    private double larguraTela = 1080; // Mock de tamanho de tela
    private double alturaTela = 1920;

    private int pontosEsquerda = 0;
    private int pontosDireita = 0;
    private volatile boolean rodando = true;

    @Override
    public void run(){
        // Loop principal do jogo (pode ser usado para atualizar o timede 60s, telemetria, etc.)
        long startTime = System.currentTimeMillis();
        while (rodando){
            if (System.currentTimeMillis() - startTime > 60000){
                rodando = false; // Fim de jogo após 60s
            }
            try{
                Thread.sleep(100);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
        // Encerrar todas as Threads filhas
        // (Lógica de encerramento omitida para brevidade)
    }

    public void adicionarAlvo(Alvo alvo){
        synchronized (lockListas){
            alvos.add(alvo);
        }
    }

    public void adicionarCanhao(double x, double y) throws JogoException{
        synchronized (lockListas){
            boolean isEsquerda = (x < larguraTela / 2.0);
            if (getQtdCanhoes(isEsquerda) >= 10){
                throw new JogoException("Limite máximo de canhões atingido para esse lado.");
            }
            Canhao c = new Canhao(x, y, this);
            canhoes.add(c);
            c.start();
        }
    }

    public void adicionarProjetil(Projetil p){
        synchronized (lockListas){
            projeteis.add(p);
        }
    }

    public void removerProjetil(Projetil p){
        synchronized (lockListas){
            projeteis.remove(p);
        }
    }

    // Região Crítica: Colisão garantindo queapenas 1 projétil valide por vez
    public void verificarColisao(Projetil p){
        synchronized (lockColisao){
            synchronized (lockListas){
                for (int i = 0; i < alvos.size(); i++){
                    Alvo alvo = alvos.get(i);
                    double dx = p.getX() - alvo.getX();
                    double dy = p.getY() - alvo.getY();
                    double distancia = Math.sqrt(dx * dx + dy * dy);

                    if (distancia < alvo.getRaio()){
                        alvo.destruir();
                        p.destruir();
                        alvos.remove(i);
                        projeteis.remove(p);

                        // Pontuação
                        if (p.isLadoEsquerdo()){
                            pontosEsquerda++;
                        } else {
                            pontosDireita++;
                        }
                        break; // Projétil só acerta um alvo
                    }
                }
            }
        }
    }

    public Alvo getAlvoMaisProximo(double cX, double cY, boolean isLadoEsquerdo){
        synchronized (lockListas){
            Alvo maisProximo = null;
            double menorDistancia = Double.MAX_VALUE;

            // Demonstração de Polimorfismo: percorrendo lista de superclasse
            for (Alvo alvo : alvos) {
                // Alo deve estar no mesmo lado do canhão
                boolean alvoNaEsquerda = (alvo.getX() < larguraTela / 2.0);
                if (alvoNaEsquerda != isLadoEsquerdo) {
                    continue;
                }

                double dx = cX - alvo.getX();
                double dy = cY - alvo.getY();
                double distancia = Math.sqrt(dx * dx + dy * dy);

                if (distancia < menorDistancia) {
                    menorDistancia = distancia;
                    maisProximo = alvo;
                }
            }
            return maisProximo;
        }
    }

    public int getQtdCanhoes(boolean isLadoEsquerdo){
        int count = 0;
        synchronized (lockListas){
            for (Canhao c : canhoes){
                if (c.isLadoEsquerdo() == isLadoEsquerdo){
                    count++;
                }
            }
        }
        return count;
    }

    // Atualiza a posição de tudo na tela
    public void atualizar(){
        for (Alvo alvo : alvos){
            if (alvo.isAtivo()){
                alvo.mover();
            }
        }
    }

    public double getLarguraTela(){
        return larguraTela;
    }
    public double getAlturaTela(){
        return alturaTela;
    }
    public List<Alvo> getAlvos() { return alvos; }
    public List<Canhao> getCanhoes() { return canhoes; }
    public List<Projetil> getProjeteis() { return projeteis; }
}
