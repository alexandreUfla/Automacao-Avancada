package com.example.autotarget;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import classes.AlvoComum;
import classes.AlvoRapido;
import classes.GameView;
import classes.Jogo;
import classes.RealTimeAnalyzer;
import classes.RealTimeTask;

public class MainActivity extends Activity {
    private GameView gameView;
    private Jogo jogo;
    private volatile boolean atualizandoPlacar = true;
    private boolean partidaJogada = false;

    // Componentes da nossa tela
    private TextView textAbatesA;
    private TextView textAbatesB;
    private TextView textEnergiaA;
    private TextView textEnergiaB;
    private TextView textVencedor;
    private Button btnIniciar;
    private Button btnAddCanhaoA;
    private Button btnRemCanhaoA;
    private Button btnAddCanhaoB;
    private Button btnRemCanhaoB;
    private ProgressBar progressEnergiaA;
    private ProgressBar progressEnergiaB;

    // Componentes de Navegação e Análise
    private Button btnNavJogo;
    private Button btnNavAnalise;
    private LinearLayout layoutJogo;
    private LinearLayout layoutAnalise;
    
    private TableLayout tableTasks;
    private TextView textLiuLayland;
    private TextView textRiStatus;
    private TextView textAmdahl;
    private TextView textTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Usa o XML desenhado na activity_main.xml
        setContentView(R.layout.activity_main);

        // Mapear elementos do Jogo
        FrameLayout container = findViewById(R.id.gameContainer);
        textAbatesA = findViewById(R.id.textAbatesA);
        textAbatesB = findViewById(R.id.textAbatesB);
        textEnergiaA = findViewById(R.id.textEnergiaA);
        textEnergiaB = findViewById(R.id.textEnergiaB);
        btnIniciar = findViewById(R.id.btnIniciar);
        btnAddCanhaoA = findViewById(R.id.btnAddCanhaoA);
        btnRemCanhaoA = findViewById(R.id.btnRemCanhaoA);
        btnAddCanhaoB = findViewById(R.id.btnAddCanhaoB);
        btnRemCanhaoB = findViewById(R.id.btnRemCanhaoB);
        progressEnergiaA = findViewById(R.id.progressEnergiaA);
        progressEnergiaB = findViewById(R.id.progressEnergiaB);
        textVencedor = findViewById(R.id.textVencedor);
        textTimer = findViewById(R.id.textTimer);
        
        // Mapear elementos de Navegação
        btnNavJogo = findViewById(R.id.btnNavJogo);
        btnNavAnalise = findViewById(R.id.btnNavAnalise);
        layoutJogo = findViewById(R.id.layoutJogo);
        layoutAnalise = findViewById(R.id.layoutAnalise);

        // Mapear elementos de Análise
        tableTasks = findViewById(R.id.tableTasks);
        textLiuLayland = findViewById(R.id.textLiuLayland);
        textRiStatus = findViewById(R.id.textRiStatus);
        textAmdahl = findViewById(R.id.textAmdahl);

        // Inicializar a lógica do jogo e vizual
        jogo = new Jogo();
        gameView = new GameView(this, jogo);

        // Colocar o desenho do jogo dentro do FrameLayout do XML
        container.addView(gameView, 0);

        // Configurar os cliques dos botões
        ConfigurarBotoes();
        ConfigurarNavegacao();
        AtualizarAnaliseTempoReal();

        // Atualizador manual de Placar
        new Thread(() -> {
            while (atualizandoPlacar) {
                // runOnUiThread força a mudança visual a acontecer na Thread principal
                runOnUiThread(() -> {
                    if (jogo != null) {
                        textAbatesA.setText("Abates: " + jogo.getPontosEsquerda());
                        textAbatesB.setText("Abates: " + jogo.getPontosDireita());
                        textEnergiaA.setText("Energia: " + jogo.getEnergiaEsquerda());
                        textEnergiaB.setText("Energia: " + jogo.getEnergiaDireita());
                        progressEnergiaA.setProgress(jogo.getEnergiaEsquerda());
                        progressEnergiaB.setProgress(jogo.getEnergiaDireita());

                        if (jogo.isRodando()) {
                            long tempoRestante = jogo.getTempoRestanteSegundos();
                            textTimer.setText(tempoRestante + "s");

                            // Atualizar dados de análise dinamicamente caso a aba esteja aberta
                            if (layoutAnalise.getVisibility() == View.VISIBLE) {
                                AtualizarAnaliseTempoReal();
                            }
                        } else {
                            textTimer.setText("60s");
                        }

                        if (!jogo.isRodando() && partidaJogada) {
                            btnIniciar.setText("Jogar Novamente (60s)");
                            btnIniciar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")));
                            textVencedor.setVisibility(View.VISIBLE);
                            if (jogo.getPontosEsquerda() > jogo.getPontosDireita()) {
                                textVencedor.setText("SISTEMA A VENCEU!");
                            } else if (jogo.getPontosDireita() > jogo.getPontosEsquerda()) {
                                textVencedor.setText("SISTEMA B VENCEU!");
                            } else {
                                textVencedor.setText("EMPATE!");
                            }
                        } else if (!partidaJogada) {
                            // Garante que o texto de vencedor esteja escondido antes de jogar
                            textVencedor.setVisibility(View.GONE);
                            btnIniciar.setText("Iniciar Batalha");
                        }
                    }
                });
                try {
                    Thread.sleep(100); // Atualiza o placar a cada 100ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void ConfigurarNavegacao() {
        btnNavJogo.setOnClickListener(v -> {
            layoutJogo.setVisibility(View.VISIBLE);
            layoutAnalise.setVisibility(View.GONE);
            btnNavJogo.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#BB86FC")));
            btnNavAnalise.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#333333")));
        });

        btnNavAnalise.setOnClickListener(v -> {
            layoutJogo.setVisibility(View.GONE);
            layoutAnalise.setVisibility(View.VISIBLE);
            btnNavAnalise.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#BB86FC")));
            btnNavJogo.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#333333")));
            AtualizarAnaliseTempoReal(); // Atualiza a aba ao abrir
        });
    }

    private void AtualizarAnaliseTempoReal() {
        List<RealTimeTask> tasks = RealTimeAnalyzer.getMockTasks(jogo);
        RealTimeAnalyzer.calcularRiExato(tasks);

        // Limpar tabela (mantendo o cabeçalho index 0)
        int childCount = tableTasks.getChildCount();
        if (childCount > 1) {
            tableTasks.removeViews(1, childCount - 1);
        }

        boolean riStatusOk = true;

        for (RealTimeTask t : tasks) {
            TableRow row = new TableRow(this);
            row.setPadding(8, 8, 8, 8);

            String[] dados = {
                t.nome, 
                String.valueOf(t.p), 
                String.valueOf(t.c), 
                String.valueOf(t.d), 
                String.valueOf(t.j), 
                String.valueOf(t.r)
            };

            for (String val : dados) {
                TextView tv = new TextView(this);
                tv.setText(val);
                tv.setTextColor(android.graphics.Color.WHITE);
                tv.setPadding(8, 8, 8, 8);
                row.addView(tv);
            }

            tableTasks.addView(row);

            if (t.r > t.d) {
                riStatusOk = false;
            }
        }

        double u = RealTimeAnalyzer.calcularUtilizacao(tasks);
        double ull = RealTimeAnalyzer.calcularLiuLayland(tasks.size());
        boolean rmStatus = u <= ull;

        textLiuLayland.setText(String.format("Utilização (U) = %.4f\nLimite Liu-Layland = %.4f\nStatus RM: %s", u, ull, rmStatus ? "OK" : "FALHOU"));
        textRiStatus.setText("Status Exato (Ri <= Di): " + (riStatusOk ? "OK (Escalonável)" : "FALHOU (Não escalonável)"));

        // Simulação Amdahl com afinidade baseada no Jogo
        int coresSimulados = 2; // OtimizadorMock aplica máscara para 2 cores
        double fracaoParalel = 0.8; // 80%
        double speedup = RealTimeAnalyzer.calcularAmdahlSpeedup(fracaoParalel, coresSimulados);
        textAmdahl.setText(String.format("Núcleos (Threads/Afinidade): %d\nFração Paralelizável: %d%%\nSpeedup Teórico: %.2fx", coresSimulados, (int)(fracaoParalel*100), speedup));

        classes.RealTimeGraphView graphView = findViewById(R.id.graphView);
        if (graphView != null) {
            graphView.setTasks(tasks);
        }
    }

    private void ConfigurarBotoes(){
        // Dinâmica do botão Iniciar e Reiniciar
        btnIniciar.setOnClickListener(v -> {
            if (jogo == null || !jogo.isRodando()) {
                partidaJogada = true;
                // Esconde o letreiro de vencedor
                textVencedor.setVisibility(View.GONE);

                // Destruimos o jogo velho e criamos um novo
                jogo = new Jogo();
                gameView.setJogo(jogo);

                // Como a partida começou, o botão "vira" um botão de abortar vermelho
                btnIniciar.setText("Encerrar Batalha");
                btnIniciar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D32F2F")));

                // Libera o jogo para iniciar novamente
                jogo.start();
            } else {
                jogo.encerrar();
                Toast.makeText(this, "Batalha encerrada!", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Ao clicar em +A
        btnAddCanhaoA.setOnClickListener(v -> {
            try {
                // Posiciona aleatóriamente na Zona Esquerda (10% a 40% da tela) para nãoficarem grudados
                double posX = jogo.getLarguraTela() * (0.1 + Math.random() * 0.3);
                double posY = jogo.getAlturaTela() - 20;
                jogo.adicionarCanhao(posX,posY);
            } catch (Exception e) { // Trata a excessão de limite máximo
                Toast.makeText(this,"Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Ao clicar em -A
        btnRemCanhaoA.setOnClickListener(v -> {
            if (jogo != null && jogo.isRodando()) {
                jogo.removerCanhao(true);
            }
        });

        // Ao clicar em +B
        btnAddCanhaoB.setOnClickListener(v -> {
            try {
                // Posiciona aleatóriamente na Zona Direita (60% a 90% da tela)
                double posX = jogo.getLarguraTela() * (0.6 + Math.random() * 0.3);
                double posY = jogo.getAlturaTela() - 20;
                jogo.adicionarCanhao(posX,posY);
            } catch (Exception e) { // Trata a excessão de limite máximo
                Toast.makeText(this,"Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Ao clicar em -B
        btnRemCanhaoB.setOnClickListener(v -> {
            if (jogo != null && jogo.isRodando()) {
                jogo.removerCanhao(false);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (gameView != null) { gameView.iniciarDesenho(); } // Liga o motor visual quando abrimos o app
    }

    @Override
    public void onPause(){
        super.onPause();
        if (gameView != null) { gameView.pausarDesenho(); } // Pausa o motor visual se o app for minimizado
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        atualizandoPlacar = false;
        if (jogo != null) {
            jogo.encerrar();
        }
    }
}