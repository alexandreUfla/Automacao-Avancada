package com.example.autotarget;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
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
    private Button btnIniciar;
    private Button btnCanhaoA;
    private Button btnCanhaoB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Usa o XML desenhado na activity_main.xml
        setContentView(R.layout.activity_main);

        // Mapear os elementos do XML para o Java usando IDs
        FrameLayout container = findViewById(R.id.gameContainer);
        textAbatesA = findViewById(R.id.textAbatesA);
        textAbatesB = findViewById(R.id.textAbatesB);
        btnIniciar = findViewById(R.id.btnIniciar);
        btnCanhaoA = findViewById(R.id.btnCanhaoA);
        btnCanhaoB = findViewById(R.id.btnCanhaoB);
        
        // Inicializar a lógica do jogo e vizual
        jogo = new Jogo();
        gameView = new GameView(this, jogo);

        // Colocar o desenho do jogo dentro do FrameLayout do XML
        container.addView(gameView);

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

                        if (!jogo.isRodando() && !btnIniciar.isEnabled()) {
                            btnIniciar.setEnabled(true); // Re-habilita o botão
                            btnIniciar.setText("Jogar Novamente (60s)");
                            btnIniciar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")));
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
                // Destruimos o jogo velho e criamos um novo
                jogo = new Jogo();
                gameView.setJogo(jogo);

                // Trava a interface durante os 60 segundos vitais
                btnIniciar.setEnabled(false);
                btnIniciar.setText("Batalha de máquinas em andamento...");
                btnIniciar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#707070ff")));

                // Libera o jogo para iniciar novamente
                jogo.start();
            } else {
                Toast.makeText(this, "Aguarde os 60 segundos para a partida acabar!", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Ao clicar em ADD Canhão A
        btnCanhaoA.setOnClickListener(v -> {
            try {
                // Posiciona aleatóriamente na Zona Esquerda (10% a 40% da tela) para nãoficarem grudados
                double posX = jogo.getLarguraTela() * (0.1 + Math.random() * 0.3);
                double posY = jogo.getAlturaTela() - 20;
                jogo.adicionarCanhao(posX,posY);
            } catch (Exception e) { // Trata a excessão de limite máximo
                Toast.makeText(this,"Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Ao clicar em ADD Canhão B
        btnCanhaoB.setOnClickListener(v -> {
            try {
                // Posiciona aleatóriamente na Zona Direita (60% a 90% da tela)
                double posX = jogo.getLarguraTela() * (0.6 + Math.random() * 0.3);
                double posY = jogo.getAlturaTela() - 20;
                jogo.adicionarCanhao(posX,posY);
            } catch (Exception e) { // Trata a excessão de limite máximo
                Toast.makeText(this,"Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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