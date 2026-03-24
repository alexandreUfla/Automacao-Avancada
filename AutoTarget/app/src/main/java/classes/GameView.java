package classes;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements Runnable {
    private Thread threadDesenho;
    private volatile boolean desenhando = false;
    private Jogo jogo;
    private SurfaceHolder prancheta;

    private Paint paintAlvoComum, paintAlvoRapido, paintCanhao, paintProjetil, paintLinha;

    public GameView(Context context, Jogo jogo){
        super(context);
        this.jogo = jogo;
        this.prancheta = getHolder(); // Pega o controle da prancheta

        // Configurando as cores de cada elemento
        paintAlvoComum = new Paint();
        paintAlvoComum.setColor(Color.BLUE);

        paintAlvoRapido = new Paint();
        paintAlvoRapido.setColor(Color.RED);

        paintCanhao = new Paint();
        paintCanhao.setColor(Color.GREEN);

        paintProjetil = new Paint();
        paintProjetil.setColor(Color.YELLOW);

        paintLinha = new Paint();
        paintLinha.setColor(Color.WHITE);
        paintLinha.setStrokeWidth(5); // Linha grossa para dividir o campo
    }

    // Métodoque roda em loop dezenas de vezes por segundo
    @Override
    public void run(){
        while(desenhando){
            // Só desenha se a prancheta estiver pronta
            if (!prancheta.getSurface().isValid()){
                continue;
            }

            // 1. "Tranca" a tela para começar a desenhar
            Canvas canvas = prancheta.lockCanvas();

            // 2. Limpa a tela pintando o fundo de preto
            canvas.drawColor(Color.BLACK);

            // 3. Desenha a linha divisória no meio
            float meioX = canvas.getWidth() / 2f;

            // 4. Desenha os Alvos (Círculos)
            // Usamos try-catch pois a lista pode mudar enquanto desenhamos
            try{
                for(Alvo alvo : jogo.getAlvos()){
                    if (alvo.isAtivo()){
                        Paint cor = (alvo instanceof AlvoRapido) ? paintAlvoRapido : paintAlvoComum;
                        canvas.drawCircle((float) alvo.getX(), (float) alvo.getY(), (float) alvo.getRaio(), cor);

                    }
                }

                // 5. Desenha os Canhões (Vou representá-los como quadrados para facilitar no começo)
                for (Canhao canhao : jogo.getCanhoes()){
                    canvas.drawRect((float) canhao.getX() - 20, (float) canhao.getY() - 20, (float) canhao.getX() + 20, (float) canhao.getY() + 20, paintCanhao);
                }

                // 6. Desenha os Projéteis (Círculos pequenos)
                for (Projetil p : jogo.getProjeteis()){
                    canvas.drawCircle((float) p.getX(), (float) p.getY(), 5, paintProjetil);
                }
            } catch (Exception e){
                // Ignora erros de concorrência na hora de desenhar para não travar o jogo
            }

            // 7. "Destranca" a tela e mostra o desenho final para o jogador
            prancheta.unlockCanvasAndPost(canvas);
        }
    }

    // Liga a thread de desenho
    public void iniciarDesenho(){
        desenhando = true;
        threadDesenho = new Thread(this);
        threadDesenho.start();
    }

    // Desliga a thread de desenho (quando o app é minimizado, por exemplo)
    public void pausarDesenho(){
        desenhando = false;
        try {
            threadDesenho.join();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
