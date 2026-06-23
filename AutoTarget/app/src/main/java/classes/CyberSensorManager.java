package classes;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Random;

public class CyberSensorManager implements SensorEventListener {

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
        }
    }

    public void start() {
        if (!useMock && tempSensor != null) {
            sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // Inicia thread de variação de temperatura mockada (sobe aos poucos)
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(5000);
                        // Temperatura varia aleatoriamente tendendo a subir
                        currentTemp += (random.nextDouble() * 2) - 0.5; 
                        
                        // Resfria magicamente se passar de 45 pra não quebrar a escala
                        if (currentTemp > 45) currentTemp = 35;
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
    }

    public double getCurrentTemp() {
        return currentTemp;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            currentTemp = event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
