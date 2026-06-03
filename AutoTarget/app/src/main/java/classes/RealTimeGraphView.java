package classes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class RealTimeGraphView extends View {
    private List<RealTimeTask> tasks = new ArrayList<>();
    private Paint paintText;
    private Paint paintRi;
    private Paint paintDi;
    private Paint paintBackground;

    public RealTimeGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintText = new Paint();
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(30f);
        paintText.setAntiAlias(true);

        paintRi = new Paint();
        paintRi.setColor(Color.parseColor("#03DAC5")); // Teal
        paintRi.setStyle(Paint.Style.FILL);

        paintDi = new Paint();
        paintDi.setColor(Color.parseColor("#CF6679")); // Error Red/Pink
        paintDi.setStyle(Paint.Style.STROKE);
        paintDi.setStrokeWidth(5f);

        paintBackground = new Paint();
        paintBackground.setColor(Color.parseColor("#1E1E1E"));
        paintBackground.setStyle(Paint.Style.FILL);
    }

    public void setTasks(List<RealTimeTask> tasks) {
        this.tasks = tasks;
        invalidate(); // Força o redesenho
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();

        // Fundo
        canvas.drawRect(0, 0, width, height, paintBackground);

        if (tasks == null || tasks.isEmpty()) {
            canvas.drawText("Sem dados de tarefas para exibir.", 50, height / 2f, paintText);
            return;
        }

        // Título do gráfico
        canvas.drawText("Gráfico de Pior Tempo de Resposta (Ri) vs Deadline (Di)", 20, 40, paintText);

        int numBars = tasks.size();
        float marginX = 100f;
        float marginBottom = 50f;
        float graphHeight = height - marginBottom - 80f; // 80 de margem superior pro titulo
        
        // Encontrar o valor máximo para a escala (o maior entre R_i e D_i de todas as tarefas)
        double maxValue = 0;
        for (RealTimeTask t : tasks) {
            if (t.d > maxValue) maxValue = t.d;
            if (t.r > maxValue) maxValue = t.r;
        }

        if (maxValue == 0) maxValue = 1; // Prevenir divisão por zero

        float barWidth = (width - marginX - 20) / (numBars * 2f);
        float spacing = barWidth;

        // Desenhar eixo X e Y
        Paint axisPaint = new Paint();
        axisPaint.setColor(Color.LTGRAY);
        axisPaint.setStrokeWidth(3f);
        
        float originY = height - marginBottom;
        float originX = marginX;
        
        // Linha Y
        canvas.drawLine(originX, 80f, originX, originY, axisPaint);
        // Linha X
        canvas.drawLine(originX, originY, width - 20, originY, axisPaint);

        // Desenhar marcações no eixo Y
        canvas.drawText(String.valueOf((int)maxValue), 10, 100, paintText);
        canvas.drawText("0", 10, originY, paintText);

        float currentX = originX + spacing / 2f;

        for (RealTimeTask t : tasks) {
            // Calcular altura da barra de Ri
            float rHeight = (float) (t.r / maxValue) * graphHeight;
            float rTop = originY - rHeight;

            // Calcular altura da linha de Di
            float dHeight = (float) (t.d / maxValue) * graphHeight;
            float dTop = originY - dHeight;

            // Desenhar barra do Ri (Preenchida)
            canvas.drawRect(currentX, rTop, currentX + barWidth, originY, paintRi);

            // Desenhar linha indicadora do Di (Vazada/Stroke)
            canvas.drawLine(currentX - 10, dTop, currentX + barWidth + 10, dTop, paintDi);

            // Desenhar nome da tarefa no eixo X
            paintText.setTextSize(24f);
            canvas.drawText(t.nome, currentX, height - 10, paintText);
            paintText.setTextSize(30f);

            // Desenhar valor numérico de Ri acima da barra
            canvas.drawText(String.valueOf((int)t.r), currentX, rTop - 10, paintText);

            currentX += barWidth + spacing;
        }

        // Legenda simplificada
        paintText.setTextSize(22f);
        canvas.drawText("Barra Teal = Ri (Tempo de Resposta)", width - 400, 40, paintText);
        canvas.drawText("Linha Rosa = Di (Deadline)", width - 400, 70, paintText);
    }
}
