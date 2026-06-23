package com.example.autotarget;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import classes.DatabaseService;

public class RankingActivity extends Activity {

    private TableLayout tableRanking;
    private ProgressBar progressRanking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        tableRanking = findViewById(R.id.tableRanking);
        progressRanking = findViewById(R.id.progressRanking);
        Button btnVoltar = findViewById(R.id.btnVoltar);

        btnVoltar.setOnClickListener(v -> finish());

        carregarRanking();
    }

    private void carregarRanking() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Usuário não logado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseService.obterRanking(uid, new DatabaseService.RankingCallback() {
            @Override
            public void onResult(List<DatabaseService.RankingItem> ranking) {
                runOnUiThread(() -> {
                    progressRanking.setVisibility(View.GONE);
                    preencherTabela(ranking);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    progressRanking.setVisibility(View.GONE);
                    Toast.makeText(RankingActivity.this, "Erro ao carregar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void preencherTabela(List<DatabaseService.RankingItem> ranking) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

        for (DatabaseService.RankingItem item : ranking) {
            TableRow row = new TableRow(this);
            row.setPadding(8, 8, 8, 8);

            String[] dados = {
                    sdf.format(new Date(item.timestamp)),
                    String.valueOf(item.pontuacao),
                    String.valueOf(item.alvosAbatidos),
                    item.configCanhoes
            };

            for (String val : dados) {
                TextView tv = new TextView(this);
                tv.setText(val);
                tv.setTextColor(android.graphics.Color.WHITE);
                tv.setPadding(8, 8, 8, 8);
                row.addView(tv);
            }

            tableRanking.addView(row);
        }
    }
}
