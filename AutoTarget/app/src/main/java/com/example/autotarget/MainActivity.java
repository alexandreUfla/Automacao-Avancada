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
    
    private classes.CyberSensorManager sensorManager;

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

        // Cyber-Physical System: Sensor de Temperatura
        sensorManager = new classes.CyberSensorManager(this);
        sensorManager.start();

        // Thread Ciberfísica de Telemetria (A cada 10s pro Firebase)
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                    if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                        classes.DatabaseService.salvarTelemetria(uid, sensorManager.getCurrentTemp());
                    }
                } catch (InterruptedException e) { break; }
            }
        }).start();

        // Inicializar a lógica do jogo e vizual
        jogo = new Jogo(sensorManager);
        gameView = new GameView(this, jogo);

        // Colocar o desenho do jogo dentro do FrameLayout do XML
        container.addView(gameView, 0);

        // Configurar os cliques dos botões
        ConfigurarBotoes();
        ConfigurarNavegacao();
        ConfigurarSpinners();
        AtualizarAnaliseTempoReal();

        // Configurar Ranking
        Button btnRanking = findViewById(R.id.btnRanking);
        btnRanking.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, RankingActivity.class);
            startActivity(intent);
        });

        // Atualizador manual de Placar
        new Thread(() -> {
            while (atualizandoPlacar) {
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
                            btnRanking.setVisibility(View.VISIBLE);
                            
                            int ptsEsquerda = jogo.getPontosEsquerda();
                            int ptsDireita = jogo.getPontosDireita();
                            
                            if (ptsEsquerda > ptsDireita) {
                                textVencedor.setText("SISTEMA A VENCEU!");
                            } else if (ptsDireita > ptsEsquerda) {
                                textVencedor.setText("SISTEMA B VENCEU!");
                            } else {
                                textVencedor.setText("EMPATE!");
                            }

                            // Salva a partida no firebase
                            if (!this.partidaSalva) {
                                this.partidaSalva = true;
                                if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                                    String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    int myScore = Math.max(ptsEsquerda, ptsDireita);
                                    int totalAbates = ptsEsquerda + ptsDireita;
                                    String cfg = "2 Nucleos Simulados";
                                    classes.DatabaseService.salvarPartida(uid, System.currentTimeMillis(), myScore, totalAbates, cfg, new classes.DatabaseService.DatabaseCallback() {
                                        @Override
                                        public void onSuccess() {
                                            android.widget.Toast.makeText(MainActivity.this, "Partida salva no nuvem!", android.widget.Toast.LENGTH_SHORT).show();
                                        }
                                        @Override
                                        public void onError(Exception e) {}
                                    });
                                }
                            }

                        } else if (!partidaJogada) {
                            textVencedor.setVisibility(View.GONE);
                            btnRanking.setVisibility(View.GONE);
                            btnIniciar.setText("Iniciar Batalha");
                        }
                    }
                });
                try {
                    Thread.sleep(100);
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

    private void ConfigurarSpinners() {
        android.widget.Spinner spinnerNucleos = findViewById(R.id.spinnerNucleos);
        android.widget.Spinner spinnerAlvos = findViewById(R.id.spinnerAlvos);
        
        if (spinnerNucleos != null) {
            String[] nucleosOpts = {"1", "2", "4"};
            android.widget.ArrayAdapter<String> adapterN = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nucleosOpts);
            adapterN.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerNucleos.setAdapter(adapterN);
            spinnerNucleos.setSelection(1); // Default to 2 cores
            
            spinnerNucleos.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (layoutAnalise.getVisibility() == View.VISIBLE) AtualizarAnaliseTempoReal();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
        
        if (spinnerAlvos != null) {
            String[] alvosOpts = {"10", "20", "50", "100"};
            android.widget.ArrayAdapter<String> adapterA = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, alvosOpts);
            adapterA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerAlvos.setAdapter(adapterA);
            spinnerAlvos.setSelection(0); // Default to 10 targets
            
            spinnerAlvos.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (layoutAnalise.getVisibility() == View.VISIBLE) AtualizarAnaliseTempoReal();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
    }

    private void AtualizarAnaliseTempoReal() {
        List<RealTimeTask> tasks = RealTimeAnalyzer.getMockTasks(jogo);
        RealTimeAnalyzer.calcularRiExato(tasks);

        // Limpar tabela (mantendo o cabeçalho index 0)
        int childCount = tableTasks.getChildCount();
        if (childCount > 1) {
            tableTasks.removeViews(1, childCount - 1);
        }

        boolean limitPass = true;

        for (RealTimeTask t : tasks) {
            TableRow row = new TableRow(this);
            row.setPadding(8, 8, 8, 8);

            if (t.r > t.d) limitPass = false;

            String[] dados = {
                t.nome, 
                t.p+"ms", 
                t.c+"ms", 
                t.d+"ms", 
                t.j+"ms", 
                t.r+"ms"
            };

            for (String d : dados){
                TextView tv = new TextView(this);
                tv.setText(d);
                tv.setTextColor(android.graphics.Color.LTGRAY);
                tv.setPadding(8,8,8,8);
                row.addView(tv);
            }
            tableTasks.addView(row);
        }

        double u = RealTimeAnalyzer.calcularUtilizacao(tasks);
        double ull = RealTimeAnalyzer.calcularLiuLayland(tasks.size());
        boolean rmStatus = u <= ull;

        textLiuLayland.setText(String.format("Utilização (U) = %.4f\nLimite (U_LL) = %.4f\nStatus RM: %s", 
                u, ull, (rmStatus ? "ESCALONÁVEL" : "NÃO ESCALONÁVEL")));

        textLiuLayland.setTextColor(rmStatus ? android.graphics.Color.GREEN : android.graphics.Color.RED);

        textRiStatus.setText(limitPass ? "Status R_i: DENTRO DO DEADLINE" : "Status R_i: DEADLINE PERDIDO");
        textRiStatus.setTextColor(limitPass ? android.graphics.Color.GREEN : android.graphics.Color.RED);

        // Atualizar Amdahl com os valores do Spinner
        int nucleos = 1;
        int qtdAlvos = 10;
        
        android.widget.Spinner spinnerNucleos = findViewById(R.id.spinnerNucleos);
        android.widget.Spinner spinnerAlvos = findViewById(R.id.spinnerAlvos);
        
        if (spinnerNucleos != null && spinnerNucleos.getSelectedItem() != null) {
            nucleos = Integer.parseInt(spinnerNucleos.getSelectedItem().toString());
        }
        if (spinnerAlvos != null && spinnerAlvos.getSelectedItem() != null) {
            qtdAlvos = Integer.parseInt(spinnerAlvos.getSelectedItem().toString());
        }

        double speedup = classes.RealTimeAnalyzer.calcularAmdahl(nucleos, qtdAlvos);
        double pEmpirico = classes.RealTimeAnalyzer.getPEmpirico(qtdAlvos);

        textAmdahl.setText(String.format(java.util.Locale.getDefault(), "Núcleos: %d | Alvos: %d\nFração Paralelizável (P): %.1f%%\nSpeedup (S): %.2fx", nucleos, qtdAlvos, pEmpirico * 100, speedup));

        classes.RealTimeGraphView graphView = findViewById(R.id.graphView);
        if (graphView != null) {
            graphView.setTasks(tasks);
        }
    }

    // Variáveis de controle de estado do placar
    private boolean partidaSalva = false;

    private void ConfigurarBotoes(){
        // Dinâmica do botão Iniciar e Reiniciar
        btnIniciar.setOnClickListener(v -> {
            if (jogo == null || !jogo.isRodando()) {
                partidaJogada = true;
                partidaSalva = false; // Reseta estado

                // Esconde o letreiro de vencedor e botão de ranking
                textVencedor.setVisibility(View.GONE);
                findViewById(R.id.btnRanking).setVisibility(View.GONE);

                // Destruimos o jogo velho e criamos um novo
                jogo = new Jogo(sensorManager);
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