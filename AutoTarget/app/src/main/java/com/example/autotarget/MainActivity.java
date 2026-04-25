package com.example.autotarget;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import classes.AlvoComum;
import classes.AlvoRapido;
import classes.GameView;
import classes.Jogo;

public class MainActivity extends Activity {
    private GameView gameView;
    private Jogo jogo;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Usa o XML desenhado na activity_main.xml
        setContentView(R.layout.activity_main);

        // Mapear os elementos do XML para o Java usando IDs
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
        
        // Inicializar a lógica do jogo e vizual
        jogo = new Jogo();
        gameView = new GameView(this, jogo);

        // Colocar o desenho do jogo dentro do FrameLayout do XML
        container.addView(gameView, 0);

        // Configurar os cliques dos botões
        ConfigurarBotoes();

        // Atualizador manual de Placar
        new Thread(() -> {
            while (true) {
                // runOnUiThread força a mudança visual a acontecer na Thread principal
                runOnUiThread(() -> {
                    if (jogo != null) {
                        textAbatesA.setText("Abates: " + jogo.getPontosEsquerda());
                        textAbatesB.setText("Abates: " + jogo.getPontosDireita());
                        textEnergiaA.setText("Energia: " + jogo.getEnergiaEsquerda());
                        textEnergiaB.setText("Energia: " + jogo.getEnergiaDireita());
                        progressEnergiaA.setProgress(jogo.getEnergiaEsquerda());
                        progressEnergiaB.setProgress(jogo.getEnergiaDireita());

                        if (!jogo.isRodando()) {
                            btnIniciar.setText("Jogar Novamente (60s)");
                            btnIniciar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")));
                            textVencedor.setVisibility(android.view.View.VISIBLE);
                            if (jogo.getPontosEsquerda() > jogo.getPontosDireita()) {
                                textVencedor.setText("SISTEMA A VENCEU!");
                            } else if (jogo.getPontosDireita() > jogo.getPontosEsquerda()) {
                                textVencedor.setText("SISTEMA B VENCEU!");
                            } else {
                                textVencedor.setText("EMPATE!");
                            }
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

    private void ConfigurarBotoes(){
        // Dinâmica do botão Iniciar e Reiniciar
        btnIniciar.setOnClickListener(v -> {
            if (jogo == null || !jogo.isRodando()) {
                // Esconde o letreiro de vencedor
                textVencedor.setVisibility(android.view.View.GONE);

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
}