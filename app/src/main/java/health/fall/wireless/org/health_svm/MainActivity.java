package health.fall.wireless.org.health_svm;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import butterknife.BindView;
import butterknife.ButterKnife;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    private SensorManager mSensorManager;
    private Sensor senAccelerometer;
    private Sensor magnetometer;
    private boolean isFall;

    private String TAG = "Health App";

    private double minAcceleration = Double.MAX_VALUE;
    private double maxAcceleration = Double.MIN_VALUE;
    private double minimum_acceleration_time;
    private double maximum_acceleration_time;
    private double previous_time = System.currentTimeMillis();
    double time_interval;

    private double sampling_interval = 1000L;

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

            double current_acceleration = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
            double current_time = System.currentTimeMillis();

            if (current_time - previous_time < sampling_interval) {
                if (current_acceleration < minAcceleration) {
                    // min acceleration
                    minAcceleration = current_acceleration;
                    minimum_acceleration_time = current_time;
//                    Log.d(TAG, "Fall Detection" + "min acceleration time: " + minimum_acceleration_time);
                }
                if (current_acceleration > maxAcceleration) {
                    // max acceleration
                    maxAcceleration = current_acceleration;
                    maximum_acceleration_time = current_time;
//                    Log.d(TAG, "Fall Detection" + "max acceleration time: " + maximum_acceleration_time);
                }

                // time interval
                time_interval = Math.abs(maximum_acceleration_time - minimum_acceleration_time);
            } else {
                // test for thye fall
                boolean result = testForFall(minAcceleration, maxAcceleration, time_interval);

//                Log.d(TAG, "Fall Detection" + "current acceleration: " + current_acceleration);
//                Log.d(TAG, "Fall Detection" + "minimum acceleration: " + minAcceleration + " maximum acceleration: " + maxAcceleration + " time difference: " + time_interval);

                Log.d(TAG, "Fall Detection" + "result: " + result);


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
                    builder.append(" " + 0 + ":" + minAcceleration);
                    builder.append(" " + 1 + ":" + maxAcceleration);
                    builder.append(" " + 2 + ":" + time_interval);
                    builder.append("\n");
                    osw.write(builder.toString());
                    osw.flush();
                    osw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                previous_time = current_time;
                minAcceleration = Double.MAX_VALUE;
                maxAcceleration = Double.MIN_VALUE;
                time_interval = 0;
            }

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public boolean testForFall(double min_acceleration, double max_acceleration, double time_interval) {
        // call svm function
        int predict_probability = 0;
        svm_model model = null;
        try {
            model = svm.svm_load_model(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Health/model");
            if (model == null) {
                System.err.print("can't open model file " + "\n");
//                System.exit(1);
                return false;
            }
            if (predict_probability == 1) {
                if (svm.svm_check_probability_model(model) == 0) {
                    System.err.print("Model does not support probabiliy estimates\n");
                    System.exit(1);
                }
            } else {
                if (svm.svm_check_probability_model(model) != 0) {
                    Predict.info("Model supports probability estimates, but disabled in prediction.\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return predict(min_acceleration, max_acceleration, time_interval, model, predict_probability);
    }

    public boolean predict(double min_acceleration, double max_acceleration, double time_interval, svm_model model, int predict_probability) {

        int svm_type = svm.svm_get_svm_type(model);
        int nr_class = svm.svm_get_nr_class(model);
        double[] prob_estimates = null;

        if (predict_probability == 1) {
            if (svm_type == svm_parameter.EPSILON_SVR ||
                    svm_type == svm_parameter.NU_SVR) {
                Predict.info("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma=" + svm.svm_get_svr_probability(model) + "\n");
            } else {
                int[] labels = new int[nr_class];
                svm.svm_get_labels(model, labels);
                prob_estimates = new double[nr_class];
            }
        }

        double v;
        svm_node[] x = new svm_node[3];
        x[0] = new svm_node();
        x[0].index = 0;
        x[0].value = min_acceleration;

        x[1] = new svm_node();
        x[1].index = 1;
        x[1].value = max_acceleration;

        x[2] = new svm_node();
        x[2].index = 2;
        x[2].value = time_interval;

//        x[0] = new svm_node();
//        x[0].index = 0;
//        x[0].value = 0.19198459048001787;
//
//        x[1] = new svm_node();
//        x[1].index = 1;
//        x[1].value = 15.25503765559154;
//
//        x[2] = new svm_node();
//        x[2].index = 2;
//        x[2].value = 123.0;

        v = svm.svm_predict(model, x);

        Log.d(TAG, "Fall Detection" + "v value: " + v);

        return v == 1;
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

//            boolean result = testForFall(0.20194491850727353, 16.01032153730474, 123.0);

            Train train = new Train();
            String[] argv = new String[3];
            argv[0] = "svm_train";
            argv[0] = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Documents/train.txt";
            argv[1] = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Health/model";
            try {
                train.run(argv);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
