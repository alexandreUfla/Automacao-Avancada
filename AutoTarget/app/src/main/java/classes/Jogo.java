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
    private int energiaEsquerda = 100;
    private int energiaDireita = 100;
    private volatile boolean rodando = false;

    @Override
    public void run(){
        rodando = true;
        // Loop principal do jogo e cronômetro (pode ser usado para atualizar o timede 60s, telemetria, etc.)
        long startTime = System.currentTimeMillis();
        long ultimoSpawn = startTime;
        long ultimoSpawnEnergia = startTime;

        while (rodando){
            long agora = System.currentTimeMillis();
            if (agora - startTime > 60000) { // Fim de jogo
                rodando = false;
            }
            if (agora - ultimoSpawn > 2000) { // Spawn de alvos a cada 2 segundos
                double randomX = Math.random() * larguraTela;
                double randomY = Math.random() * (alturaTela / 2); // Nasce do "céu"

                Alvo novoAlvo;
                if (Math.random() > 0.3) {
                    novoAlvo = new AlvoComum(randomX, randomY, this);
                } else {
                    novoAlvo = new AlvoRapido(randomX, randomY, this); // 30% de chance de ser o alvo rápido
                }

                adicionarAlvo(novoAlvo);
                novoAlvo.start(); // Inicia a thread do alvo

                ultimoSpawn = agora;
            }

            // Sistema de Energia (Drena 1 ponto por cada canhão a cada 1 segundo)
            if (agora - ultimoSpawnEnergia > 1000) {
                energiaEsquerda -= getQtdCanhoes(true);
                energiaDireita -= getQtdCanhoes(false);

                // Não deixa a energia ficar negativa
                if (energiaEsquerda < 0) energiaEsquerda = 0;
                if (energiaDireita < 0) energiaDireita = 0;

                ultimoSpawnEnergia = agora;
            }

            try {
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

    public double getLarguraTela(){
        return larguraTela;
    }
    public double getAlturaTela(){
        return alturaTela;
    }

    public void setLarguraTela(double largura){
        this.larguraTela = largura;
    }
    public void setAlturaTela(double altura){
        this.alturaTela = altura;
    }

    // Cópias seguras das listas usando cadeado
    public List<Alvo> getAlvos() { 
        synchronized (lockListas) {
            return new ArrayList<>(alvos);
        } 
    }
    public List<Canhao> getCanhoes() {
        synchronized (lockListas) {
            return new ArrayList<>(canhoes);
        } 
    }
    public List<Projetil> getProjeteis() { 
        synchronized (lockListas) {
            return new ArrayList<>(projeteis);
        } 
    }

    // Método para a tela conseguir ler quem está ganhando
    public int getPontosEsquerda(){
        return pontosEsquerda;
    }
    public int getPontosDireita(){
        return pontosDireita;
    }

    public boolean isRodando(){
        return rodando;
    }

    public int getEnergiaEsquerda(){
        return energiaEsquerda;
    }

    public int getEnergiaDireita(){
        return energiaDireita;
    }

    // Procura na lista do último canhão daquele lado, manda desligar a sua thread e remove!
    public void removerCanhao (boolean esquerda) {
        synchronized (lockListas) {
            for (int i = canhoes.size() - 1; i >= 0; i--) {
                Canhao c = canhoes.get(i);
                if (c.isLadoEsquerdo() == esquerda) {
                    c.desligar(); // Mata a linha de execução daquele canhão
                    canhoes.remove(i);
                    break;
                }
            }
        }
    }
}
