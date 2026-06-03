package classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RealTimeAnalyzer {
    
    public static List<RealTimeTask> getMockTasks(Jogo jogo) {
        List<RealTimeTask> tasks = new ArrayList<>();
        
        int qtdAlvos = 0;
        int qtdCanhoes = 0;
        int qtdProjeteis = 0;
        
        if (jogo != null) {
            qtdAlvos = jogo.getAlvos().size();
            qtdCanhoes = jogo.getCanhoes().size();
            qtdProjeteis = jogo.getProjeteis().size();
        }

        // Custo base + peso dinâmico de acordo com os objetos na tela
        double cOtimizador = 20 + (qtdAlvos * qtdCanhoes * 2); 
        double cFisica = 5 + (qtdAlvos * qtdProjeteis * 0.5); 
        double cRender = 5 + ((qtdAlvos + qtdCanhoes + qtdProjeteis) * 1.5); 

        // Clamp para não passar absurdamente do período
        cOtimizador = Math.min(cOtimizador, 999);
        cFisica = Math.min(cFisica, 99);
        cRender = Math.min(cRender, 32);

        // Nome, P, C, D, J
        tasks.add(new RealTimeTask("Otimizador", 1000, cOtimizador, 1000, 0));
        tasks.add(new RealTimeTask("Física/Colisão", 100, cFisica, 100, 5));
        tasks.add(new RealTimeTask("Renderização", 33, cRender, 33, 2));
        
        // O RM ordena por menor período
        Collections.sort(tasks, Comparator.comparingDouble(t -> t.p));
        return tasks;
    }

    public static double calcularUtilizacao(List<RealTimeTask> tasks) {
        double u = 0;
        for (RealTimeTask t : tasks) {
            u += (t.c / t.p);
        }
        return u;
    }

    public static double calcularLiuLayland(int n) {
        if (n == 0) return 0;
        return n * (Math.pow(2.0, 1.0 / n) - 1.0);
    }

    public static void calcularRiExato(List<RealTimeTask> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            RealTimeTask ti = tasks.get(i);
            double rAtual = ti.c;
            double rAnterior = 0;
            
            int maxIterations = 100;
            int iter = 0;

            while (rAtual != rAnterior && iter < maxIterations) {
                rAnterior = rAtual;
                double interferencia = 0;
                
                // Tarefas de prioridade maior (como já está ordenado por RM, são as anteriores)
                for (int j = 0; j < i; j++) {
                    RealTimeTask tj = tasks.get(j);
                    interferencia += Math.ceil((rAnterior + tj.j) / tj.p) * tj.c;
                }
                
                rAtual = ti.c + interferencia;
                
                if (rAtual > ti.d) {
                    break; // Passou do deadline, não escalonável
                }
                iter++;
            }
            ti.r = rAtual;
        }
    }

    public static double calcularAmdahlSpeedup(double fracaoParalelizavel, int nucleos) {
        if (nucleos <= 0) return 1.0;
        return 1.0 / ((1.0 - fracaoParalelizavel) + (fracaoParalelizavel / nucleos));
    }
}
