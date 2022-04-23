package com.example.smartcarmqttapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.Observable;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.smartcarmqttapp.state.CarState;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import pl.droidsonroids.gif.GifImageView;
import java.time.Duration;
import java.time.LocalTime;

public class PracticeDrivingActivity extends AppCompatActivity {
    public MqttCar controller;

    //Camera Config
    private final int IMAGE_HEIGHT = 240;
    private final int IMAGE_WIDTH = 320;
    public ImageView imageView;

    public GifImageView screenError;



    private Button sensorDisplayButton;
    private BottomNavigationView bottomNavigationView;
    private Dialog sensorDialog;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_driving);

        sensorDisplayButton = findViewById(R.id.sensorDataButton);
        sensorDialog = new Dialog(this);




        sensorDisplayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                openSensorDialog();
            }

        });


        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.practiceDriving);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.connectedCar:
                        startActivity(new Intent(getApplicationContext(), ConnectedCarActivity.class));
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.practiceDriving:
                        return true;

                    case R.id.home:
                        startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.practiceTheory:
                        startActivity(new Intent(getApplicationContext(), PracticeTheoryActivity.class));
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.aboutUs:
                        startActivity(new Intent(getApplicationContext(), AboutUsActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                }
                return false;
            }
        });


        controller = new MqttCar(getApplicationContext(), () -> {
            try {
                controller.changeSpeed(0.5);
            } catch (MqttException ex) {
                ex.printStackTrace();
            }
        }, this);


        imageView = findViewById(R.id.cameraView);

        //screenError = findViewById(R.id.screenError);

        /*
        if(CarState.instance.isConnected()) {
            imageView.setVisibility(View.VISIBLE);
            //screenError.setVisibility(View.GONE);
        }
        else {
            imageView.setVisibility(View.INVISIBLE);
            //screenError.setVisibility(View.VISIBLE);
        }
         */

    }


    /**
     *
     * @param message of frames to be rendered
     * This should be called upon received a message on the Camera Topic
     * and should then update the ImageView displayed on the current screen
     */
    public void cameraRendering(MqttMessage message){
        final Bitmap bm = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);

        final byte[] payload = message.getPayload();
        final int[] colors = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
        for (int ci = 0; ci < colors.length; ++ci) {
            final byte r = payload[3 * ci ];
            final byte g = payload[3 * ci + 1];
            final byte b = payload[3 * ci + 2];
            colors[ci] = Color.rgb(r, g, b);
        }

        bm.setPixels(colors, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        imageView.setImageBitmap(bm);

    }

    /**
     * Constants for determining Car behavior
     */
    public static final class ControlConstant {

        enum ChangeMode {
            ADDITION, MULTIPLICATION;
        }

        public static final double STARTING_SPEED = 10; // new speed of car when accelerating with no speed (percentage integer)
        public static final double INITIAL_SPEED = 0; // speed of car upon initialization
        public static final double INITIAL_ANGLE = 0; // angle of car upon initialization

        public static final double ACCELERATION_FACTOR = 1.1; // multiplication factor for accelerating
        public static final double DECELERATION_FACTOR = 0.9; // multiplication factor for decelerating

        public static final double TURN_LEFT_ANGLE = 10; // addition angle for turning left
        public static final double TURN_RIGHT_ANGLE = -10; // addition angle for turning right

        public static final double MIN_SPEED = 0.05; // threshold for stopping car when decelerating

        public static final ChangeMode ANGLE_CHANGE = ChangeMode.ADDITION;
        public static final ChangeMode SPEED_CHANGE = ChangeMode.MULTIPLICATION;

    }

    /**
     * Prefixes for displaying Sensor readings
     */
    public static final class SensorString {
        public static final String SPEED = "Car Speed: ";
        public static final String DISTANCE = "Total Distance: ";
        public static final String GYROSCOPE = "Gyroscope Heading: ";
        public static final String BLINKER = "Blinker Status: ";
        public static final String INFRARED = "Infrared Distance: ";
        public static final String ULTRASONIC = "Ultrasonic Distance: ";
    }

    public static final boolean FORCE_UPDATE = false; // upon theoretical data change, immediately updates visible fields
    public static final double GYROSCOPE_OFFSET = 180;

    /* ToDo:
     * Bind buttons to methods
     * Add method bodies
     * Update text views
     * Create UI
     * If doesn't work, move debugging statements before controller access
     * Test functionality
     * VERY IMPORTANT: #changeSpeed takes throttle percentage integer (20), but speed.get() returns m/s.
     * When calculating accelerated speed, use map for (speed in m/s) -> (speed in %) to throttle properly.
     */

    /**
     * Increases (multiplication) speed of moving car OR begin movement of standing car. Bound to button R.id.upButton
     */
    public void onClickAccelerate(View view) throws MqttException {
        double initialSpeed = controller.speed.get(); // returns 0.138
        initialSpeed = getThrottleFromAbsoluteSpeed(initialSpeed); // returns 10
        double acceleratedSpeed = initialSpeed == 0 ? ControlConstant.STARTING_SPEED : initialSpeed * ControlConstant.ACCELERATION_FACTOR;
        controller.changeSpeed(acceleratedSpeed);

        if(FORCE_UPDATE) controller.speed.set(acceleratedSpeed);

        // Debugging
        System.out.println("Accelerating from " + initialSpeed + " % to " + acceleratedSpeed + " %");

        /* unopinionated approach to changing speed and angle by allowing user to select change mode.
        double acceleratedSpeed =
                ControlConstant.SPEED_CHANGE == ControlConstant.ChangeMode.MULTIPLICATION ?
                        initialSpeed * ControlConstant.ACCELERATION_FACTOR :
                        initialSpeed + ControlConstant.ACCELERATION_FACTOR;
         */
    }

    /**
     * Decreases (multiplication) speed of moving car OR stops movement given speed is below threshold. Bound to button R.id.downButton
     */
    public void onClickDecelerate(View view) throws MqttException {
        double initialSpeed = controller.speed.get();
        initialSpeed = getThrottleFromAbsoluteSpeed(initialSpeed);
        double deceleratedSpeed = initialSpeed > ControlConstant.MIN_SPEED ? initialSpeed * ControlConstant.DECELERATION_FACTOR : 0;
        controller.changeSpeed(deceleratedSpeed);

        if(FORCE_UPDATE) controller.speed.set(deceleratedSpeed);

        // Debugging
        System.out.println("Accelerating from " + initialSpeed + " % to " + deceleratedSpeed + " %");
    }

    /**
     * Increases (addition) wheel angle of car. Bound to button R.id.leftButton
     */
    public void onClickRotateLeft(View view) throws MqttException {
//        double initialAngle = controller.gyroscopeHeading.get();
        double initialAngle = controller.wheelAngle.get();
        double rotatedAngle = initialAngle + ControlConstant.TURN_LEFT_ANGLE;
        controller.steerCar(rotatedAngle);
        controller.wheelAngle.set(rotatedAngle);

        if(FORCE_UPDATE) controller.gyroscopeHeading.set(rotatedAngle - GYROSCOPE_OFFSET);

        // Debugging
        System.out.println("Rotating Right from " + initialAngle + " deg to " + rotatedAngle + " deg");
    }

    /**
     * Decreases (addition) wheel angle of car. Bound to button R.id.rightButton
     */
    public void onClickRotateRight(View view) throws MqttException {
//        double initialAngle = controller.gyroscopeHeading.get();
        double initialAngle = controller.wheelAngle.get();
        double rotatedAngle = initialAngle + ControlConstant.TURN_RIGHT_ANGLE;
        controller.steerCar(rotatedAngle);
        controller.wheelAngle.set(rotatedAngle);

        if(FORCE_UPDATE) controller.gyroscopeHeading.set(rotatedAngle - GYROSCOPE_OFFSET);

        // Debugging
        System.out.println("Rotating Left from " + initialAngle + " deg to " + rotatedAngle + " deg");
    }

    /**
     * Begins blinking left. Bound to button ###
     * UI Implementation left for Milestone 6: Manual Driving
     */
    public void onClickBlinkLeft(View view) throws MqttException {
        controller.blinkDirection(MqttCar.BlinkerDirection.Left);

        if(FORCE_UPDATE) controller.blinkerStatus.set(MqttCar.BlinkerDirection.Left);
    }

    /**
     * Begins blinking right. Bound to button ###
     * UI Implementation left for Milestone 6: Manual Driving
     */
    public void onClickBlinkRight(View view) throws MqttException {
        controller.blinkDirection(MqttCar.BlinkerDirection.Right);

        if(FORCE_UPDATE) controller.blinkerStatus.set(MqttCar.BlinkerDirection.Right);
    }

    /**
     * Stops blinking. Bound to button ###
     * UI Implementation left for Milestone 6: Manual Driving
     */
    public void onClickBlinkOff(View view) throws MqttException {
        controller.blinkDirection(MqttCar.BlinkerDirection.Off);

        if(FORCE_UPDATE) controller.blinkerStatus.set(MqttCar.BlinkerDirection.Off);
    }

    /**
     * Engages Emergency Stop and stops car. Bound to button ###
     * UI Implementation left for Milestone 6: Manual Driving
     */
    public void onClickEmergencyStop(View view) throws MqttException {
        controller.emergencyStop();
    }

    // Utility methods

    public double getThrottleFromAbsoluteSpeed(double absoluteSpeed) {
        // Throttle and absolute speed are approximately linearly correlated with k=1.8
        // To obtain percentage integer from ratio, multiply by 100
        return absoluteSpeed / 1.8 * 100;
    }

    private void openSensorDialog() {
        sensorDialog.setContentView(R.layout.sensor_dialog);
        sensorDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        sensorDialog.show();
        ImageView closeDialog = sensorDialog.findViewById(R.id.closeDialog);

        TextView speedValue = sensorDialog.findViewById(R.id.speedField);
        TextView distanceValue = sensorDialog.findViewById(R.id.distanceField);
        TextView USValue = sensorDialog.findViewById(R.id.ultrasoundField);
        TextView gyroHeading = sensorDialog.findViewById(R.id.gyroHeadingField);
        TextView infraredValue = sensorDialog.findViewById(R.id.infraredDistance);


        Thread newThread = new Thread() {
            @Override
            public void run(){
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Set speed value
                                speedValue.setText(CarState.instance.getSpeed());

                                // Set distance
                                distanceValue.setText(CarState.instance.getDistance());

                                // Set Ultrasound reading
                                USValue.setText(CarState.instance.getUltraSoundDistance());

                                // Set Gyroscope heading
                                gyroHeading.setText(CarState.instance.getGyroHeading());

                                // Set Infrared reading
                                infraredValue.setText(CarState.instance.getIRDistance());
                            }
                        });

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        newThread.start();

        closeDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sensorDialog.dismiss();
            }
        });

    }

}