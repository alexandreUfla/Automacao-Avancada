package classes;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Random;

public class CyberSensorManager implements SensorEventListener {

    private static final String TAG = "CyberSensor";

    private final SensorManager sensorManager;
    private final Sensor tempSensor;
    private double currentTemp = 30.0;
    private boolean useMock = false;
    private final Random random = new Random();

    public CyberSensorManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        if (tempSensor == null) {
            // Maioria dos celulares não tem termômetro ambiente, usaremos mock
            useMock = true;
            Log.w(TAG, "[CPS] Sensor TYPE_AMBIENT_TEMPERATURE não encontrado no hardware. Ativando modo MOCK (simulação).");
        } else {
            Log.i(TAG, "[CPS] Sensor de temperatura ambiente detectado: " + tempSensor.getName());
        }
    }

    public void start() {
        if (!useMock && tempSensor != null) {
            sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.i(TAG, "[CPS] Listener de temperatura física registrado. Aguardando leituras do hardware...");
        } else {
            Log.i(TAG, "[CPS] Modo MOCK ativo. Iniciando thread de simulação de temperatura.");
            // Inicia thread de variação de temperatura mockada (sobe aos poucos)
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(5000);
                        // Temperatura varia aleatoriamente tendendo a subir
                        currentTemp += (random.nextDouble() * 2) - 0.5;

                        // Resfria magicamente se passar de 45 pra não quebrar a escala
                        if (currentTemp > 45) currentTemp = 35;

                        // LOG DE TELEMETRIA - aparece no Logcat a cada 5s
                        Log.d(TAG, String.format("[CPS Telemetria] Temp atual: %.1f°C | Status: %s",
                                currentTemp,
                                currentTemp >= 40.0 ? "THROTTLING ATIVO (>40°C)" : "Normal"));

                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
        }
    }

    public void stop() {
        if (!useMock) {
            sensorManager.unregisterListener(this);
        }
        Log.i(TAG, "[CPS] SensorManager encerrado.");
    }

    public double getCurrentTemp() {
        return currentTemp;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            currentTemp = event.values[0];
            Log.d(TAG, String.format("[CPS Hardware] Leitura do sensor físico: %.1f°C", currentTemp));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
