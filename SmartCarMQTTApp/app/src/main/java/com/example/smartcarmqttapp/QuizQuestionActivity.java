package com.example.smartcarmqttapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class QuizQuestionActivity extends AppCompatActivity {

    private TextView questionCount;
    private TextView questionsLeft;
    private TextView scoreText;
    private TextView timer;
    private ImageView questionImage;
    private TextView explanationText;
    private static int scoreNumber = 0;

    //Radio buttons
    private RadioGroup radioGroup;
    private RadioButton option1;
    private RadioButton option2;
    private RadioButton option3;
    private RadioButton option4;

    //correct answer choice from radio group (1,2,3, or 4)
    private int correctAnswer;
    private int clicks = 0;

    private Drawable right;
    private Drawable wrong;

    private TooltipCompat tooltipCompat;

    private BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_question);

        right = getDrawable(R.drawable.correct_border);
        wrong = getDrawable(R.drawable.wrong_border);

        questionCount = findViewById(R.id.questionCount);
        questionsLeft = findViewById(R.id.questionsLeft);
        scoreText = findViewById(R.id.score);
        scoreText.setText(Integer.toString(scoreNumber));
        timer = findViewById(R.id.header);
        questionImage = findViewById(R.id.questionImage);

        //Radio buttons
        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        radioGroup = findViewById(R.id.radioGroup);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.practiceTheory);

        onNextQuestionButtonClicked();

        //TODO for @Lancear: Add a listener/if statement that essentially asks the user whether they are sure
        //TODO for @Lancear: Add dialog box with 2 buttons: yes -> driving theory screen, no -> back to current question
        //TODO for @Lancear: Decide where exactly dialog comes up: clicking bottom navigation bar, ... or another quit button at each question?
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.connectedCar:
                        startActivity(new Intent(getApplicationContext(), ConnectedCarActivity.class));
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.practiceDriving:
                        startActivity(new Intent(getApplicationContext(), PracticeDrivingActivity.class));
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.home:
                        startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.practiceTheory:
                        return true;

                    case R.id.aboutUs:
                        startActivity(new Intent(getApplicationContext(), AboutUsActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                }
                return false;
            }
        });
    }

    public void onNextQuestionButtonClicked() {
        Button finishQuizButton = findViewById(R.id.nextQuestionBTN);
        Button checkAnswerBtn = findViewById(R.id.checkAnswer);
        correctAnswer = option1.getId();

        //TODO: set the correct answer, based on query, to have onclick listener with explanation
        option1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                option1.setTooltipText("Ipsum lorens, this should explain the nature of why the chosen option is correct");
            }
        });

        checkAnswerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //After confirmation of answer, cant select any other question
                option1.setClickable(false);
                option2.setClickable(false);
                option3.setClickable(false);
                option4.setClickable(false);

                Drawable drawable = getDrawable(R.drawable.button_border);
                checkAnswerBtn.setBackground(drawable);

                //for testing option 1 is correct

                //switch case for setting style of correct answer
                if(radioGroup.getCheckedRadioButtonId() == correctAnswer){
                    scoreNumber++;
                }

                switch(radioGroup.getCheckedRadioButtonId()) {
                    case R.id.option1:
                        withBorderOpt1();
                        break;
                    case R.id.option2:
                        withBorderOpt2();
                        break;
                    case R.id.option3:
                        withBorderOpt3();
                        break;
                    case R.id.option4:
                        withBorderOpt4();
                        break;
                }
            }
        });

        finishQuizButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //if radio buttons are disabled or there were two clicks on next question button (skip)
                if(!option1.isClickable() || clicks == 1){
                    clicks = 0;
                    startActivity(new Intent(QuizQuestionActivity.this, PracticeTheoryActivity.class));
                    //When the amount of questions finish
                    if (questionCount.getText().equals(questionsLeft.getText())) {
                        //when the question count finished, go to the results screen
                        startActivity(new Intent());
                        //TODO: call results screen and set the back or exit button to go back to home screen
                    }

                    if (timer.getText().equals("0:00")){
                        //TODO for @Lancear: add logic for when the timer reaches zero -> goes to result screen
                        //TODO for @Lancear: move this in a method where it loops, checking timer until it reaches zero (talk to ivan about it)
                    }

                    //go to next question
                    //after checks for timer and
                }
                else{
                    //Set text to say: please confirm an answer or click again to skip
                    TextView areYouSure = findViewById(R.id.areYouSure);
                    areYouSure.setText("Are you sure you want to skip?");
                    clicks++;
                }
            }
        });
    }

    public void withBorderOpt1(){

        option1.setBackground(right);
        option1.setTypeface(null, Typeface.BOLD);
        option2.setBackground(wrong);
        option3.setBackground(wrong);
        option4.setBackground(wrong);
    }

    public void withBorderOpt2(){

        option1.setBackground(wrong);
        option2.setBackground(right);
        option2.setTypeface(null, Typeface.BOLD);
        option3.setBackground(wrong);
        option4.setBackground(wrong);
    }

    public void withBorderOpt3(){

        option1.setBackground(wrong);
        option2.setBackground(wrong);
        option3.setBackground(right);
        option3.setTypeface(null, Typeface.BOLD);
        option4.setBackground(wrong);
    }

    public void withBorderOpt4(){

        option1.setBackground(wrong);
        option2.setBackground(wrong);
        option3.setBackground(wrong);
        option4.setBackground(right);
        option4.setTypeface(null, Typeface.BOLD);
    }
}