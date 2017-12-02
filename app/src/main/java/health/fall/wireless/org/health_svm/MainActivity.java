package health.fall.wireless.org.health_svm;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import health.fall.wireless.org.health_svm.model.Features;
import health.fall.wireless.org.health_svm.model.FixedQueue;
import health.fall.wireless.org.health_svm.utils.Util;
import libsvm.svm_problem;
import umich.cse.yctung.androidlibsvm.LibSVM;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    private SensorManager mSensorManager;
    private Sensor senAccelerometer;
    private Sensor magnetometer;
    private FixedQueue queue;
    private boolean isFall;

    @BindView(R.id.train_button)
    Button train_button;
    @BindView(R.id.predict_button)
    Button predict_button;
    @BindView(R.id.train_radio_btn_layout)
    LinearLayout train_radio_btn_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_UI);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        queue = new FixedQueue(Util.QUEUE_SIZE);

        train_button.setOnClickListener(this);
        predict_button.setOnClickListener(this);
        train_radio_btn_layout.setVisibility(View.INVISIBLE);

        isFall = false;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            final float x = sensorEvent.values[0];
            final float y = sensorEvent.values[1];
            final float z = sensorEvent.values[2];

            double smv = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
            double sma;

            // Sma calculation
            float sum_x = 0, sum_y = 0, sum_z = 0;
            Iterator iterator = queue.iterator();
            while (iterator.hasNext()) {
                Features n = (Features) iterator.next();
                sum_x += Math.abs(n.getX());
                sum_y += Math.abs(n.getY());
                sum_z += Math.abs(n.getZ());
            }
            sum_x += Math.abs(x);
            sum_y += Math.abs(y);
            sum_z += Math.abs(z);
            sma = (sum_x + sum_y + sum_z) / Util.QUEUE_SIZE;

            // add features to queue
            Features features = new Features(x, y, z, smv, sma);
            queue.add(features);

            try {
                // get the path to sdcard
                File sdcard = Environment.getExternalStorageDirectory();
                // to this path add a new directory path
                File dir = new File(sdcard.getAbsolutePath() + "/Health/");
                // create this directory if not already created
                dir.mkdir();
                // create the file in which we will write the contents
                File file = new File(dir, "train.txt");
                file.createNewFile();
                FileOutputStream fOut = new FileOutputStream(file, true);
                OutputStreamWriter osw = new OutputStreamWriter(fOut);
                StringBuilder builder = new StringBuilder();
                builder.append("1");
                int i = 0;
                for (Features f : queue) {
                    builder.append(" " + i + ":" + f.getX());
                    i++;
                    builder.append(" " + i + ":" + f.getY());
                    i++;
                    builder.append(" " + i + ":" + f.getZ());
                    i++;
                    builder.append(" " + i + ":" + f.getSma());
                    i++;
                    builder.append(" " + i + ":" + f.getSmv());
                    i++;
                }
                builder.append("\n");
                osw.write(builder.toString());
                osw.flush();
                osw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.train_button) {
            isFall = true;
            train_radio_btn_layout.setVisibility(View.VISIBLE);

//            Train train = new Train();
//            String[] argv = new String[3];
////        argv[0] = "svm_train";
//            argv[0] = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Health/train.txt";
//            argv[1] = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Health/model";
//            try {
//                train.run(argv);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        } else if (view.getId() == R.id.predict_button) {
            train_radio_btn_layout.setVisibility(View.INVISIBLE);
//            Predict predict = new Predict();
//            String[] argv = new String[3];
//            argv[0] = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Health/train.txt";
//            argv[1] = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Health/model";
//            argv[2] = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Health/result";
//            try {
//                predict.main(argv);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }
}
