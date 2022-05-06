package com.example.smartcarmqttapp.screens;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;

import com.example.smartcarmqttapp.MqttCar;
import com.example.smartcarmqttapp.Navigation;
import com.example.smartcarmqttapp.R;
import com.example.smartcarmqttapp.state.CarState;

import org.eclipse.paho.client.mqttv3.MqttException;
import pl.droidsonroids.gif.GifImageView;


public class PracticeDrivingActivity extends AppCompatActivity {
    public MqttCar controller;
    private int clicks = 0;
    private boolean exit = false;

    public ImageView leftBlinkerArrow, rightBlinkerArrow;
    public CardView leftBlinkerButton, rightBlinkerButton;
    public ImageView imageView;

    private TextView ultraSoundText;
    private TextView gyroText;
    private TextView infraredText;

    private TextView ultraVal;
    private TextView gyroVal;
    private TextView infraredVal;

    //For error screen when the car is not connected
    public GifImageView screenError;

    private Button toggleDataButton;
    private Dialog sensorDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_driving);
        Navigation.initializeNavigation(this, R.id.practiceDriving);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("\nUse the arrow keys to maneuver the car \n \n" +
                        "Red button is an emergency stop \n \n" +
                        "Pressing the toggle data gives some extra car data ;)")
                .setPositiveButton("Time to drive!", (theDialog, id) -> {})
                .create();

        dialog.setTitle("Quick Hints on how to drive the car");
        dialog.show();

        leftBlinkerArrow = findViewById(R.id.leftBlinkerArrow);
        rightBlinkerArrow = findViewById(R.id.rightBlinkerArrow);

        leftBlinkerButton = findViewById(R.id.leftBlinker);
        rightBlinkerButton= findViewById(R.id.rightBlinker);

        ultraSoundText = findViewById(R.id.udText);
        gyroText = findViewById(R.id.gyroText);
        infraredText = findViewById(R.id.infraText);

        ultraVal = findViewById(R.id.ultrasoundValue);
        gyroVal = findViewById(R.id.gyroValue);
        infraredVal = findViewById(R.id.infraValue);

        //sensorDisplayButton = findViewById(R.id.sensorDataButton);
        sensorDialog = new Dialog(this);

        toggleDataButton = findViewById(R.id.toggleDataBtn);

        dashboard();

        toggleDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(clicks == 0){
                    exit = false;
                    clicks++;

                    if (CarState.instance.isConnected()) {
                        CarState.instance.getConnectedCar().listeners.put("data", () -> {
                            if(!exit){
                                ultraSoundText.setText("Ultrasound Distance (cm):");
                                gyroText.setText("Gyroscope Heading (deg):");
                                infraredText.setText("Infrared Distance (cm):");
                                // Set Ultrasound reading
                                ultraVal.setText(CarState.instance.getUltraSoundDistance());

                                // Set Gyroscope heading
                                gyroVal.setText(CarState.instance.getGyroHeading());

                                // Set Infrared reading
                                infraredVal.setText(CarState.instance.getIRDistance());
                            }
                            else{
                                ultraSoundText.setText("");
                                gyroText.setText("");
                                infraredText.setText("");
                                ultraVal.setText("");
                                gyroVal.setText("");
                                infraredVal.setText("");
                            }
                        });
                    }
                }
                else {
                    exit = true;
                    clicks = 0;

                    if (CarState.instance.isConnected()) {
                        CarState.instance.getConnectedCar().listeners.remove("data");
                    }

                    ultraSoundText.setText("");
                    gyroText.setText("");
                    infraredText.setText("");
                    ultraVal.setText("");
                    gyroVal.setText("");
                    infraredVal.setText("");
                }
            }
        });

        imageView = findViewById(R.id.cameraView);
        screenError = findViewById(R.id.screenError);

        if (CarState.instance.isConnected()) {
            imageView.setVisibility(View.VISIBLE);
            screenError.setVisibility(View.GONE);

            controller = CarState.instance.getConnectedCar();
            controller.listeners.put("camera", () -> {
                    Log.d("ui update", "ui update");
                runOnUiThread(() -> {
                    imageView.setImageBitmap(controller.camera.get());
                });
            });
        }
        else {
            imageView.setVisibility(View.INVISIBLE);
            screenError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStop() {
        if (CarState.instance.isConnected()) {
            CarState.instance.getConnectedCar().listeners.remove("dashboard");
            CarState.instance.getConnectedCar().listeners.remove("camera");
        }
        super.onStop();
    }

    private void dashboard(){
        TextView speedVal = findViewById(R.id.speedValue);
        TextView distanceVal = findViewById(R.id.distanceValue);

        if (CarState.instance.isConnected()) {
            CarState.instance.getConnectedCar().listeners.put("dashboard", () -> {
                speedVal.setText(CarState.instance.getSpeed() + " m/s");
                distanceVal.setText(CarState.instance.getDistance() + " cm");
            });
        }
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

        public static final double ACCELERATION_FACTOR = 1.5; // multiplication factor for accelerating
        public static final double DECELERATION_FACTOR = 0.8; // multiplication factor for decelerating

        public static final double TURN_LEFT_ANGLE = 10; // addition angle for turning left
        public static final double TURN_RIGHT_ANGLE = -10; // addition angle for turning right

        public static final double STARTING_THROTTLE = 10;
        public static final double MIN_THROTTLE = 5; // threshold for stopping car when decelerating
        public static final double MAX_THROTTLE = 100; // threshold for accelerating car

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
        double initialThrottle = controller.throttle.get();
        double acceleratedThrottle;
        if(initialThrottle == 0) { // car standing still: start driving
            acceleratedThrottle = ControlConstant.STARTING_THROTTLE;
        }else if(initialThrottle > 0) { // car driving: increase speed
            acceleratedThrottle = initialThrottle * ControlConstant.ACCELERATION_FACTOR;
        }else { // car driving backwards: increase speed (decrease speed modulus)
            acceleratedThrottle = initialThrottle / ControlConstant.ACCELERATION_FACTOR;
        }
        acceleratedThrottle = Math.min(acceleratedThrottle, ControlConstant.MAX_THROTTLE); // speed cant be over MAX
        controller.changeSpeed(acceleratedThrottle); // publishes to MQTT
        controller.throttle.set(acceleratedThrottle); // stores current throttle

        // Debugging
        System.out.println("Accelerating from " + initialThrottle + " % to " + acceleratedThrottle + " %");

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
        double initialThrottle = controller.throttle.get();
        double deceleratedThrottle;
        if(initialThrottle == 0) { // car standing still: start driving backwards
            deceleratedThrottle = -ControlConstant.STARTING_THROTTLE;
        }else if(initialThrottle > 0) { // car driving: slow down
            deceleratedThrottle = initialThrottle * ControlConstant.DECELERATION_FACTOR;
        }else{ // car driving backwards: speed up backwards
            deceleratedThrottle = initialThrottle / ControlConstant.DECELERATION_FACTOR;
        }
        // if after deceleration the speed is (positive) MIN, then car should stop
        deceleratedThrottle = deceleratedThrottle > 0 && deceleratedThrottle < ControlConstant.MIN_THROTTLE ? 0 : deceleratedThrottle;
        // speed modulus cant be greater than MAX
        deceleratedThrottle = Math.max(deceleratedThrottle, -ControlConstant.MAX_THROTTLE);
        controller.changeSpeed(deceleratedThrottle); // publish to MQTT
        controller.throttle.set(deceleratedThrottle); // store current throttle

        // Debugging
        System.out.println("Decelerating from " + initialThrottle + " to " + deceleratedThrottle);
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

        Animation animation = AnimationUtils.loadAnimation(
                getApplicationContext(),
                R.anim.blinker
        );

        if (clicks == 1) {
            clicks--;
            leftBlinkerArrow.clearAnimation();
        }else{
            leftBlinkerArrow.startAnimation(animation);
            rightBlinkerArrow.clearAnimation();
            clicks++;
        }
        if(FORCE_UPDATE) controller.blinkerStatus.set(MqttCar.BlinkerDirection.Right);
    }

    /**
     * Begins blinking right. Bound to button ###
     * UI Implementation left for Milestone 6: Manual Driving
     */
    public void onClickBlinkRight(View view) throws MqttException {
        controller.blinkDirection(MqttCar.BlinkerDirection.Right);

        Animation animation = AnimationUtils.loadAnimation(
                getApplicationContext(),
                R.anim.blinker
        );

        if (clicks == 1) {
            clicks--;
            rightBlinkerArrow.clearAnimation();
        }else{
            rightBlinkerArrow.startAnimation(animation);
            leftBlinkerArrow.clearAnimation();
            clicks++;
        }
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
}