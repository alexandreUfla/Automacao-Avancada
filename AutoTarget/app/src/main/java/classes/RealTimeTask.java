package classes;

public class RealTimeTask {
    public String nome;
    public double p; // Periodo
    public double c; // Tempo de computação
    public double d; // Deadline
    public double j; // Jitter
    public double r; // Pior tempo de resposta (calculado)

    public RealTimeTask(String nome, double p, double c, double d, double j) {
        this.nome = nome;
        this.p = p;
        this.c = c;
        this.d = d;
        this.j = j;
        this.r = 0;
    }
}
