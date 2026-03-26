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
        // Ao clicar em Iniciar
        btnIniciar.setOnClickListener(v -> {
            try {
                // Inicia a Thread do jogo e desabilita o botão para não clicar 2 vezes
                jogo.start();
                btnIniciar.setEnabled(false);
                btnIniciar.setText("Partida em andamento...");

                // Adiciona alguns alvos iniciais para testar
                jogo.adicionarAlvo(new AlvoComum(300,300,jogo));
                jogo.adicionarAlvo(new AlvoRapido(600,500,jogo));
            } catch (IllegalThreadStateException e) {
                // Se o botão for clicado e a thread já estiver rodando
                Toast.makeText(this, "A partida já começou!", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Ao clicar em + Canhão A
        btnCanhaoA.setOnClickListener(v -> {
            try {
                // Tenta adicionar no lado esquerdo da tela
                // A altura (Y) vamos colocar fixo perto do rodapé por enquanto
                jogo.adicionarCanhao(200,1500);
            } catch (Exception e) { // Trata a excessão de limite máximo
                Toast.makeText(this,"Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Ao clicar em + Canhão B
        btnCanhaoB.setOnClickListener(v -> {
            try {
                // Tenta adicionar no lado direito da tela
                // A altura (Y) vamos colocar fixo perto do rodapé por enquanto
                jogo.adicionarCanhao(800,1500);
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