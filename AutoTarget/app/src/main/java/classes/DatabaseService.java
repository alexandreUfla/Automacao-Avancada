package classes;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseService {

    private static final ExecutorService executor = Executors.newFixedThreadPool(4); // Thread pool para DB
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface DatabaseCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface RankingCallback {
        void onResult(List<RankingItem> ranking);
        void onError(Exception e);
    }

    public static class RankingItem implements Comparable<RankingItem> {
        public long timestamp;
        public int pontuacao;
        public int alvosAbatidos;
        public String configCanhoes;

        @Override
        public int compareTo(RankingItem other) {
            // Ordem decrescente de pontuação
            return Integer.compare(other.pontuacao, this.pontuacao);
        }
    }

    /**
     * Salva os dados de uma partida de forma assíncrona usando threads.
     */
    public static void salvarPartida(String uid, long timestamp, int pontuacao, int alvosAbatidos, String configCanhoes, DatabaseCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Montar JSON com dados sensíveis
                JSONObject json = new JSONObject();
                json.put("pontuacao", pontuacao);
                json.put("alvosAbatidos", alvosAbatidos);
                json.put("configCanhoes", configCanhoes);
                json.put("timestamp", timestamp);

                // 2. Criptografar
                String payload = Cryptography.encrypt(json.toString(), uid);

                if (payload == null) {
                    throw new Exception("Falha na criptografia.");
                }

                // 3. Montar map para Firestore
                Map<String, Object> docData = new HashMap<>();
                docData.put("data_criptografada", payload);
                // timestamp externo pode ser util pra debugar, mas a rubrica pede tudo criptografado
                // vamos deixar apenas a string cifrada

                // 4. Salvar na collection do user
                db.collection("usuarios").document(uid).collection("partidas")
                        .add(docData)
                        .addOnSuccessListener(docRef -> callback.onSuccess())
                        .addOnFailureListener(callback::onError);

            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Recupera o ranking descriptografando e ordenando localmente de forma assíncrona.
     */
    public static void obterRanking(String uid, RankingCallback callback) {
        executor.execute(() -> {
            try {
                db.collection("usuarios").document(uid).collection("partidas")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            // Estamos num callback do Firebase, vamos jogar de volta pro executor pra não travar UI
                            executor.execute(() -> {
                                try {
                                    List<RankingItem> lista = new ArrayList<>();
                                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                        String payload = doc.getString("data_criptografada");
                                        if (payload != null) {
                                            String decrypted = Cryptography.decrypt(payload, uid);
                                            if (decrypted != null) {
                                                JSONObject json = new JSONObject(decrypted);
                                                RankingItem item = new RankingItem();
                                                item.pontuacao = json.getInt("pontuacao");
                                                item.alvosAbatidos = json.getInt("alvosAbatidos");
                                                item.configCanhoes = json.getString("configCanhoes");
                                                item.timestamp = json.getLong("timestamp");
                                                lista.add(item);
                                            }
                                        }
                                    }
                                    
                                    // Sincronização: Ordenar a lista (maior pontuação no topo)
                                    synchronized (lista) {
                                        Collections.sort(lista);
                                    }
                                    
                                    callback.onResult(lista);
                                } catch (Exception ex) {
                                    callback.onError(ex);
                                }
                            });
                        })
                        .addOnFailureListener(callback::onError);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Salva telemetria (temperatura) para o sistema ciberfísico.
     */
    public static void salvarTelemetria(String uid, double temperatura) {
        executor.execute(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("temperatura", temperatura);
            data.put("timestamp", System.currentTimeMillis());

            db.collection("usuarios").document(uid).collection("telemetria").add(data);
        });
    }
}
