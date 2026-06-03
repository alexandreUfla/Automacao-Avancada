package classes;

import java.lang.reflect.Method;
import java.util.List;

public class OtimizadorManager extends Thread {
    private volatile boolean ativo = true;
    private Jogo jogo;

    public OtimizadorManager(Jogo jogo) {
        this.jogo = jogo;
        aplicarThreadAffinityMock(); // Tenta setar a afinidade
    }

    private void aplicarThreadAffinityMock() {
        try {
            // Usa Java Reflection para simular a chamada "Process.setThreadAffinityMask" pedida na AV2
            Class<?> processClass = Class.forName("android.os.Process");
            Method setAffinityMethod = processClass.getMethod("setThreadAffinityMask", int.class, int.class);
            int myTid = (int) processClass.getMethod("myTid").invoke(null);
            
            // Supondo que quer rodar nos 2 primeiros cores (mask = 3 -> binário 11)
            setAffinityMethod.invoke(null, myTid, 3);
            System.out.println("Thread affinity setada com sucesso (Simulação)");
        } catch (Exception e) {
            // Em caso do OS não suportar essa API internamente (o que é o comum para apps de usuário sem root)
            System.out.println("Dummy Thread Affinity aplicado. Fallback para execução normal.");
        }
    }

    @Override
    public void run() {
        while (ativo && jogo.isRodando()) {
            try {
                // Processa imediatamente na primeira iteração, depois espera 1s
                processarLado(true);
                processarLado(false);
                Thread.sleep(1000); // Atualiza posição dos canhões a cada 1s
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processarLado(boolean isEsquerda) {
        // Pega snapshot seguro dos alvos e canhões do lado analisado
        List<Alvo> alvos = jogo.getAlvos();
        List<Canhao> todosCanhoes = jogo.getCanhoes();

        // Filtra apenas os canhões do lado correto (usando a cópia defensiva do getCanhoes)
        List<Canhao> canhoes = new java.util.ArrayList<>();
        for (Canhao c : todosCanhoes) {
            if (c.isLadoEsquerdo() == isEsquerda) canhoes.add(c);
        }

        if (alvos.isEmpty() || canhoes.isEmpty()) return;

        int numAlvos = alvos.size();
        int numCanhoes = canhoes.size();
        int numMedidas = numCanhoes * numAlvos;

        double[] y_arr = new double[numMedidas];
        double[][] V_arr = new double[numMedidas][numMedidas];
        double[][] A_arr = new double[1][numMedidas];

        int index = 0;
        double[][] posicoesMediasAlvos = new double[numAlvos][2];

        // 1. Coleta estatística de cada alvo
        for (int a = 0; a < numAlvos; a++) {
            Alvo alvo = alvos.get(a);
            List<SensorVirtual.Leitura> leituras = alvo.getLeiturasRecentes();

            double mediaX, mediaY;
            if (!leituras.isEmpty()) {
                double somaX = 0, somaY = 0;
                for (SensorVirtual.Leitura l : leituras) {
                    somaX += l.x;
                    somaY += l.y;
                }
                mediaX = somaX / leituras.size();
                mediaY = somaY / leituras.size();
            } else {
                mediaX = alvo.getX();
                mediaY = alvo.getY();
            }
            posicoesMediasAlvos[a][0] = mediaX;
            posicoesMediasAlvos[a][1] = mediaY;

            // Preenche métricas para cada par (alvo, canhão)
            for (int c = 0; c < numCanhoes; c++) {
                Canhao canhao = canhoes.get(c);
                double dist = Math.sqrt(Math.pow(canhao.getX() - mediaX, 2) + Math.pow(canhao.getY() - mediaY, 2));
                y_arr[index] = dist;

                double variancia = leituras.isEmpty() ? 50.0 : calcularVarianciaDist(leituras, canhao.getX(), canhao.getY(), dist);
                if (variancia == 0) variancia = 0.1;
                V_arr[index][index] = variancia;

                double threshold = jogo.getLarguraTela() * 0.5; // 50% da largura da tela
                A_arr[0][index] = (dist < threshold) ? 1.0 : 0.0;
                index++;
            }
        }

        // 2. Aplicar Reconciliação de Dados
        double[] distanciasReconciliadas = DataReconciliation.reconcile(y_arr, V_arr, A_arr);

        // 3. Para cada canhão, encontra o alvo mais próximo e move em direção a ele (apenas eixo X)
        double meio = jogo.getLarguraTela() / 2.0;
        double margem = 30;

        // Espaçamento mínimo entre canhões (em pixels)
        final double ESPACO_MIN = 40.0;

        // Passo 3a: Calcula o novoX ideal para cada canhão
        double[] alvosX = new double[numCanhoes];
        for (int c = 0; c < numCanhoes; c++) {
            Canhao canhao = canhoes.get(c);

            double melhorDist = Double.MAX_VALUE;
            double melhorX = canhao.getX();
            for (int a = 0; a < numAlvos; a++) {
                int idx = a * numCanhoes + c;
                double dist = (idx < distanciasReconciliadas.length) ? distanciasReconciliadas[idx] : Double.MAX_VALUE;
                if (dist < melhorDist) {
                    melhorDist = dist;
                    melhorX = posicoesMediasAlvos[a][0];
                }
            }

            // Clamp: impede o canhão de cruzar para o lado inimigo
            if (canhao.isLadoEsquerdo()) {
                melhorX = Math.max(margem, Math.min(melhorX, meio - margem));
            } else {
                melhorX = Math.max(meio + margem, Math.min(melhorX, jogo.getLarguraTela() - margem));
            }
            alvosX[c] = melhorX;
        }

        // Passo 3b: Ordena os canhões pelo X desejado e aplica espaçamento mínimo
        // Cria índices ordenados por alvosX
        Integer[] indices = new Integer[numCanhoes];
        for (int i = 0; i < numCanhoes; i++) indices[i] = i;
        java.util.Arrays.sort(indices, (i1, i2) -> Double.compare(alvosX[i1], alvosX[i2]));

        double[] posFinais = new double[numCanhoes];
        posFinais[indices[0]] = alvosX[indices[0]];

        // Passa para frente: garante espaço mínimo entre consecutivos
        for (int k = 1; k < numCanhoes; k++) {
            int cur = indices[k];
            int prev = indices[k - 1];
            posFinais[cur] = Math.max(alvosX[cur], posFinais[prev] + ESPACO_MIN);
        }

        // Passa para trás: corrige possíveis extrapolações de limite
        for (int k = numCanhoes - 2; k >= 0; k--) {
            int cur = indices[k];
            int next = indices[k + 1];
            posFinais[cur] = Math.min(posFinais[cur], posFinais[next] - ESPACO_MIN);
        }

        // Passo 3c: Aplica as posições finais, garantindo que ainda estão dentro dos limites
        for (int c = 0; c < numCanhoes; c++) {
            Canhao canhao = canhoes.get(c);
            double novoX;
            if (canhao.isLadoEsquerdo()) {
                novoX = Math.max(margem, Math.min(posFinais[c], meio - margem));
            } else {
                novoX = Math.max(meio + margem, Math.min(posFinais[c], jogo.getLarguraTela() - margem));
            }
            // Atualiza apenas X; Y permanece fixo (canhões só movem horizontalmente)
            canhao.setNovaPosicao(novoX, canhao.getY());
        }

        // 4. Função de Utilidade (Adicionar / Remover Canhões)
        avaliarUtilidadeCanhoes(isEsquerda, numCanhoes, numAlvos, distanciasReconciliadas);
    }

    private double calcularVarianciaDist(List<SensorVirtual.Leitura> leituras, double cX, double cY, double mediaDist) {
        double soma = 0;
        for (SensorVirtual.Leitura l : leituras) {
            double d = Math.sqrt(Math.pow(cX - l.x, 2) + Math.pow(cY - l.y, 2));
            soma += Math.pow(d - mediaDist, 2);
        }
        return soma / leituras.size();
    }

    private void avaliarUtilidadeCanhoes(boolean isEsquerda, int numCanhoes, int numAlvos, double[] distanciasReconciliadas) {
        // Função de utilidade: Cada alvo próximo (< 300) gera "pontos esperados"
        int abatesEsperados = 0;
        for (double d : distanciasReconciliadas) {
            if (d < 300) abatesEsperados++;
        }

        double ganhoPorAbate = 10.0;
        double custoManutencao = numCanhoes * 1.0; // 1 de energia/s = 10 em 10s
        double fatorPenalidade = numCanhoes > 5 ? (1 + (numCanhoes - 5) * 0.2) : 1.0;

        double utilidadeAtual = (abatesEsperados * ganhoPorAbate) - (custoManutencao * fatorPenalidade);

        // Se a utilidade estiver muito baixa e o time tiver mais de 1 canhão, compensa remover
        if (utilidadeAtual < 0 && numCanhoes > 1) {
            jogo.removerCanhao(isEsquerda);
        } 
        // Se houver muitos alvos para os canhões atuais (e energia suficiente), adicionamos
        else if (abatesEsperados > numCanhoes * 2 && numCanhoes < 10) {
            int energia = isEsquerda ? jogo.getEnergiaEsquerda() : jogo.getEnergiaDireita();
            if (energia > 50) { // Margem de segurança
                try {
                    double novoX = isEsquerda ? (jogo.getLarguraTela() * 0.2) : (jogo.getLarguraTela() * 0.8);
                    double novoY = jogo.getAlturaTela() - 50;
                    jogo.adicionarCanhao(novoX, novoY);
                } catch (JogoException e) {
                    // Ignora, limite máximo atingido
                }
            }
        }
    }

    public void desligar() {
        this.ativo = false;
        this.interrupt();
    }
}
