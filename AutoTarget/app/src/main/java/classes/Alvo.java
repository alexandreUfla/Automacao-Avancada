package classes;

import java.util.ArrayList;
import java.util.List;

public abstract class Alvo implements Runnable {
    protected double x, y;
    protected double raio = 25;
    protected double velocidade;
    protected double dx = Math.random() > 0.5 ? 1 : -1;
    protected double dy = Math.random() > 0.5 ? 1 : -1;
    protected volatile boolean ativo = true;
    protected Jogo jogo;
    
    // Buffer de leituras do sensor (AV2)
    private List<SensorVirtual.Leitura> bufferLeituras = new ArrayList<>();

    public Alvo(double x, double y, Jogo jogo){
        this.x = x;
        this.y = y;
        this.jogo = jogo;
    }

    // Método que será sobrescrito demostrando o Polimorfismo
    public abstract void mover();
    
    // Fornece a cor do alvo de forma polimórfica para a View
    public abstract int getCor();

    @Override
    public void run(){
        long ultimoSensor = System.currentTimeMillis();
        while(ativo){
            mover();
            limitesTela();
            jogo.verificarTransferencia(this); // Checa se cruzou a fronteira atômicamente
            
            long agora = System.currentTimeMillis();
            if (agora - ultimoSensor >= 1000) {
                registrarLeituraSensor();
                ultimoSensor = agora;
            }

            try{
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void registrarLeituraSensor() {
        double lX = SensorVirtual.lerComRuido(x);
        double lY = SensorVirtual.lerComRuido(y);
        double lVX = SensorVirtual.lerComRuido(dx * velocidade);
        double lVY = SensorVirtual.lerComRuido(dy * velocidade);
        
        synchronized(bufferLeituras) {
            bufferLeituras.add(new SensorVirtual.Leitura(lX, lY, lVX, lVY));
            // Mantém apenas os últimos 10 segundos (como gravamos 1 por seg, guardamos os últimos 10)
            if (bufferLeituras.size() > 10) {
                bufferLeituras.remove(0);
            }
        }
    }
    
    public List<SensorVirtual.Leitura> getLeiturasRecentes() {
        synchronized(bufferLeituras) {
            return new ArrayList<>(bufferLeituras);
        }
    }

    private void limitesTela(){
        if (x - raio < 0) {
            x = raio;
            dx = dx * (-1);
        } else if (x + raio > jogo.getLarguraTela()) {
            x = jogo.getLarguraTela() - raio;
            dx = dx * (-1);
        }

        if (y - raio < 0) {
            y = raio;
            dy = dy * (-1);
        } else if (y + raio > jogo.getAlturaTela()) {
            y = jogo.getAlturaTela() - raio;
            dy = dy * (-1);
        }
    }

    public void destruir(){ this.ativo = false; }
    public double getX(){ return x; }
    public double getY(){ return y; }
    public double getRaio(){ return raio; }
    public boolean isAtivo(){ return ativo; }
}
