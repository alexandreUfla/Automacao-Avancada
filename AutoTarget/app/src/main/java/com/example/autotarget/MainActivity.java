package com.example.autotarget;
import android.os.Bundle;
import android.app.Activity;
import classes.AlvoComum;
import classes.AlvoRapido;
import classes.GameView;
import classes.Jogo;

public class MainActivity extends Activity {
    private GameView gameView;
    private Jogo jogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Cria a lógica do jogo e inicia a Thread principal dele
        jogo = new Jogo();
        jogo.start();

        // Adiciona alguns alvos de teste para ver na tela
        jogo.adicionarAlvo(new AlvoComum(200, 200, jogo));
        jogo.adicionarAlvo(new AlvoRapido(300, 300, jogo));

        // 2. Cria a tela visual passando a lógica do jogo para ela
        gameView = new GameView(this, jogo);

        // 3. Define que a visualização do celular será o nosso jogo
        setContentView(gameView);
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