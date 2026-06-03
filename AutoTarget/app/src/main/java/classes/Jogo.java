package classes;
import java.util.ArrayList;
import java.util.List;

public class Jogo extends Thread{
    private List<Alvo> alvosEsquerda = new ArrayList<>();
    private List<Alvo> alvosDireita = new ArrayList<>();
    private List<Canhao> canhoes = new ArrayList<>();
    private List<Projetil> projeteis = new ArrayList<>();

    // Objetos para lock (Regiões críticas)
    private final Object lockListas = new Object();
    private final Object lockColisao = new Object();

    private OtimizadorManager otimizador;

    private double larguraTela = 1080; // Mock de tamanho de tela
    private double alturaTela = 1920;

    private volatile int pontosEsquerda = 0;
    private volatile int pontosDireita = 0;
    private volatile int energiaEsquerda = 100; // AV2: Energia inicial 100
    private volatile int energiaDireita = 100;
    private volatile boolean rodando = false;

    @Override
    public void run(){
        rodando = true;
        otimizador = new OtimizadorManager(this);
        otimizador.start();
        
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
                    novoAlvo = new AlvoRapido(randomX, randomY, this);
                }

                adicionarAlvo(novoAlvo);
                novoAlvo.start(); // Inicia a thread do alvo

                ultimoSpawn = agora;
            }

            // Sistema de Energia (Drena 1 ponto por cada canhão a cada 1 segundo)
            if (agora - ultimoSpawnEnergia > 1000) {
                energiaEsquerda -= getQtdCanhoes(true);
                energiaDireita -= getQtdCanhoes(false);

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
        
        if (otimizador != null) otimizador.desligar();

        // Encerrar todas as Threads filhas adequadamente (Fix AV1)
        synchronized(lockListas) {
            for (Alvo a : alvosEsquerda) { a.destruir(); a.interrupt(); }
            for (Alvo a : alvosDireita) { a.destruir(); a.interrupt(); }
            for (Canhao c : canhoes) { c.desligar(); c.interrupt(); }
            for (Projetil p : projeteis) { p.destruir(); p.interrupt(); }
        }
    }

    public void adicionarAlvo(Alvo alvo){
        synchronized (lockListas){
            if (alvo.getX() < larguraTela / 2.0) {
                alvosEsquerda.add(alvo);
            } else {
                alvosDireita.add(alvo);
            }
        }
    }

    // Mecanismo atômico de transferência de alvo de uma lista para a outra
    public void verificarTransferencia(Alvo alvo) {
        synchronized (lockListas) {
            boolean estaNaEsquerda = (alvo.getX() < larguraTela / 2.0);
            if (estaNaEsquerda && alvosDireita.contains(alvo)) {
                alvosDireita.remove(alvo);
                alvosEsquerda.add(alvo);
            } else if (!estaNaEsquerda && alvosEsquerda.contains(alvo)) {
                alvosEsquerda.remove(alvo);
                alvosDireita.add(alvo);
            }
        }
    }

    public void adicionarCanhao(double x, double y) throws JogoException{
        Canhao c = null;
        synchronized (lockListas){
            boolean isEsquerda = (x < larguraTela / 2.0);
            if (getQtdCanhoes(isEsquerda) >= 10){
                throw new JogoException("Limite máximo de canhões atingido para esse lado.");
            }
            c = new Canhao(x, y, this);
            canhoes.add(c);
        }
        // Iniciar fora do lock (Fix AV1)
        if (c != null) {
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

    public void verificarColisao(Projetil p){
        synchronized (lockColisao){
            synchronized (lockListas){
                boolean alvoAtingido = false;
                
                // Verifica na esquerda
                for (int i = 0; i < alvosEsquerda.size(); i++){
                    Alvo alvo = alvosEsquerda.get(i);
                    double dx = p.getX() - alvo.getX();
                    double dy = p.getY() - alvo.getY();
                    double distancia = Math.sqrt(dx * dx + dy * dy);

                    if (distancia < alvo.getRaio()){
                        alvo.destruir();
                        p.destruir();
                        alvosEsquerda.remove(i);
                        projeteis.remove(p);
                        alvoAtingido = true;
                        break; 
                    }
                }
                
                // Se não atingiu na esquerda, tenta na direita
                if (!alvoAtingido) {
                    for (int i = 0; i < alvosDireita.size(); i++){
                        Alvo alvo = alvosDireita.get(i);
                        double dx = p.getX() - alvo.getX();
                        double dy = p.getY() - alvo.getY();
                        double distancia = Math.sqrt(dx * dx + dy * dy);

                        if (distancia < alvo.getRaio()){
                            alvo.destruir();
                            p.destruir();
                            alvosDireita.remove(i);
                            projeteis.remove(p);
                            alvoAtingido = true;
                            break; 
                        }
                    }
                }

                if (alvoAtingido) {
                    // Pontuação
                    if (p.isLadoEsquerdo()){
                        pontosEsquerda++;
                        energiaEsquerda += 10;
                        if (energiaEsquerda > 150) energiaEsquerda = 150;
                    } else {
                        pontosDireita++;
                        energiaDireita += 10;
                        if (energiaDireita > 150) energiaDireita = 150;
                    }
                }
            }
        }
    }

    public Alvo getAlvoMaisProximo(double cX, double cY, boolean isLadoEsquerdo){
        synchronized (lockListas) {
            Alvo maisProximo = null;
            double menorDistancia = Double.MAX_VALUE;
            
            // Pega TODOS os alvos para permitir tiros cruzados
            List<Alvo> todos = new ArrayList<>();
            todos.addAll(alvosEsquerda);
            todos.addAll(alvosDireita);

            for (Alvo alvo : todos) {
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

    public double getLarguraTela(){ return larguraTela; }
    public double getAlturaTela(){ return alturaTela; }
    public void setLarguraTela(double largura){ this.larguraTela = largura; }
    public void setAlturaTela(double altura){ this.alturaTela = altura; }

    public List<Alvo> getAlvos() { 
        synchronized (lockListas) {
            List<Alvo> todos = new ArrayList<>();
            todos.addAll(alvosEsquerda);
            todos.addAll(alvosDireita);
            return todos;
        } 
    }
    
    public List<Alvo> getAlvosLado(boolean isLadoEsquerdo) {
        synchronized(lockListas) {
            return isLadoEsquerdo ? new ArrayList<>(alvosEsquerda) : new ArrayList<>(alvosDireita);
        }
    }

    public List<Canhao> getCanhoes() {
        synchronized (lockListas) { return new ArrayList<>(canhoes); } 
    }
    public List<Projetil> getProjeteis() { 
        synchronized (lockListas) { return new ArrayList<>(projeteis); } 
    }

    public int getPontosEsquerda(){ return pontosEsquerda; }
    public int getPontosDireita(){ return pontosDireita; }
    public boolean isRodando(){ return rodando; }
    public int getEnergiaEsquerda(){ return energiaEsquerda; }
    public int getEnergiaDireita(){ return energiaDireita; }

    public void removerCanhao (boolean esquerda) {
        synchronized (lockListas) {
            for (int i = canhoes.size() - 1; i >= 0; i--) {
                Canhao c = canhoes.get(i);
                if (c.isLadoEsquerdo() == esquerda) {
                    c.desligar(); 
                    canhoes.remove(i);
                    break;
                }
            }
        }
    }

    public void encerrar () {
        this.rodando = false;
    }
}
