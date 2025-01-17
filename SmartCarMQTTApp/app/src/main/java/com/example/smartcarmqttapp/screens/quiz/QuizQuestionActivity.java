package com.example.smartcarmqttapp.screens.quiz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.smartcarmqttapp.Navigation;
import com.example.smartcarmqttapp.R;
import com.example.smartcarmqttapp.database.CrushersDataBase;
import com.example.smartcarmqttapp.database.CrushersDataBaseManager;
import com.example.smartcarmqttapp.model.Question;
import com.example.smartcarmqttapp.model.UserAnswer;
import com.example.smartcarmqttapp.screens.HomeActivity;
import com.example.smartcarmqttapp.state.QuizState;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class QuizQuestionActivity extends AppCompatActivity {

    private CrushersDataBase db;

    private TextView questionCountText;
    private TextView scoreText;
    private TextView timer;
    private ImageView questionImage;
    private Button nextButton;
    private TextView categoryText;
    private TextView questionText;
    private TextView areYouSure;

    //Radio buttons
    private RadioGroup radioGroup;
    private RadioButton option1;
    private RadioButton option2;
    private RadioButton option3;
    private RadioButton option4;
    private RadioButton explanationButton;

    //correct answer choice from radio group (1,2,3, or 4)
    private String correctAns;
    private int correctAnswer;

    private int currentQuestionNum = 0;
    private int clicks = 0;
    private int totalQuestions;
    private int scoreNumber = 0;

    private Drawable right;
    private Drawable wrong;

    private List<Question> questionList;
    private List<Question> specifcQuestionList;

    //To keep track of categories covered
    private HashSet<String> categories;
    private TooltipCompat tooltipCompat;

    private String categorySelected = "No Category";

    private BottomNavigationView bottomNavigationView;
    private QuizState quizState;

    private static int MILLIS;
    private int questionCountSelected;
    private int TOTAL_TIME;
    private QuizQuestionActivity zis;
    private Question currentQuestion;

    private Question currentQ;

    private VideoView questionVideo;

    private CrushersDataBaseManager results_db = new CrushersDataBaseManager(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_question);
        zis = this;
        Intent intent = getIntent();

        Bundle extras = intent.getExtras();
        String category = "No Category";
        int numberOfQuestions = 0;
        if (extras != null) {
            MILLIS = extras.getInt("TIMER_VALUE", 0);
            category = extras.getString("category");
            numberOfQuestions = extras.getInt("numOfQuestions");
        }

        TOTAL_TIME = MILLIS;
        if (TOTAL_TIME > 0) startCountDown();

        right = getDrawable(R.drawable.correct_border);
        wrong = getDrawable(R.drawable.wrong_border);

        questionCountText = findViewById(R.id.questionCount);
        //questionsLeftText.setText(totalQuestions);

        //View fields
        scoreText = findViewById(R.id.score);
        scoreText.setText(Integer.toString(scoreNumber));
        timer = findViewById(R.id.timer);
        nextButton = findViewById(R.id.nextQuestionBTN);
        categoryText = findViewById(R.id.categoryText);
        areYouSure = findViewById(R.id.areYouSure);
        questionText = findViewById(R.id.questionText);
        questionImage = findViewById(R.id.questionImage);

        //Radio buttons
        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        radioGroup = findViewById(R.id.radioGroup);

        //add questions to question list via helper method --> help us select question
        this.db = new CrushersDataBase(this);
        try {
            questionList = this.db.getAllQuestions();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.shuffle(questionList);
        totalQuestions = questionList.size();
        categories = new HashSet<>();

        specifcQuestionList = new ArrayList<>();
        questionVideo = findViewById(R.id.videoScreen);
        try {
            specifcQuestionList = QuizState.instance.customQuiz(numberOfQuestions, category, this.db);
            quizState = new QuizState(true, specifcQuestionList, null, 0);
            System.out.println(specifcQuestionList.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        addQuestion(specifcQuestionList);

        totalQuestions = quizState.getQuestions().size();
        currentQ = quizState.getCurrentQuestion();
        onNextQuestionButtonClicked();

        this.db.close();
        // bottomNavigation bar
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.practiceTheory);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                alertQuitQuiz(() -> Navigation.navigate(zis, item));
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        alertQuitQuiz(() -> {
            startActivity(new Intent(getApplicationContext(), HomeActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    protected void alertQuitQuiz(Runnable onQuit) {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setMessage("Are you sure, you are almost at the finish line! Let's finish this together! You can do it!")
            .setNegativeButton("🙌 Let's do this!", (theDialog, id) -> {})
            .setPositiveButton("😔 Maybe next time", (theDialog, id) -> {
                onQuit.run();
            })
        .create();

        dialog.setTitle("Leaving Quiz");
        dialog.setIcon(R.drawable.ic_baseline_follow_the_signs_24);
        dialog.show();
    }

    /**
     * instantiates two buttons: finishQuizButton and checkAnswerBtn
     *
     * @button checkAnswerBtn finalizes the user's choice and makes sure that
     * the radio buttons are disabled so that the user cannot choose another answer
     *
     * @button finishQuizButton moves on to the next question, provided the user
     * has selected an answer and clicked the checkAnswerBtn. If that is not the case
     * then the user is prompted with if they want to skip the question. If skipped, that
     * question is marked as incorrect and stored as an incorrect answer.
     */
    public void onNextQuestionButtonClicked() {
        Button nextQuestionButton = findViewById(R.id.nextQuestionBTN);
        Button checkAnswerBtn = findViewById(R.id.checkAnswer);

        if (currentQuestionNum == totalQuestions){
            nextQuestionButton.setText("Finish Quiz");
        }

        checkAnswerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView selectQ = findViewById(R.id.selectQuestion);
                areYouSure.setText("");

                // Displaying animations for those questions that have one
                if (!(currentQuestion.getAnimation() == null)) {
                    questionImage.setVisibility(View.INVISIBLE);
                    questionVideo.setVisibility(View.VISIBLE);
                    questionVideo.start();
                    initializeVideoPlayer(currentQuestion.getAnimation());
                }

                if (radioGroup.getCheckedRadioButtonId() == -1) {
                    selectQ.setText("Select an answer or skip by pressing 'Next Question' twice");
                } else {
                    selectQ.setText("");
                    //Set skip warning to transparent
                    TextView textView = findViewById(R.id.areYouSure);
                    textView.setText("");

                    //After confirmation of answer, cant select any other question
                    option1.setClickable(false);
                    option2.setClickable(false);
                    option3.setClickable(false);
                    option4.setClickable(false);

                    // show the explanation
                    Question currentQuestion = quizState.getCurrentQuestion(currentQuestionNum - 1);
                    TextView explanation = findViewById(R.id.explanation);
                    explanation.setText(currentQuestion.getExplanation());

                    //show the user that they also cant re-press the button
                    Drawable drawable = getDrawable(R.drawable.button_border);
                    checkAnswerBtn.setBackground(drawable);

                    //switch case for setting style of correct answer
                    if (radioGroup.getCheckedRadioButtonId() == correctAnswer) {
                        scoreNumber++;
                        // num and index differ by 1
                        try {
                            quizState.answerQuestion(currentQ, new UserAnswer(currentQuestionNum-1, true), false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            quizState.answerQuestion(currentQ, new UserAnswer(currentQuestionNum-1, false), false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    switch (correctAnswer) {
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
            }
        });

        nextQuestionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAnswerBtn.setBackgroundResource(android.R.drawable.btn_default);
                //if radio buttons are disabled or there were two clicks on next question button (skip)
                areYouSure.setText("");

                if(!option1.isClickable() || clicks == 1){
                    if (currentQuestionNum == totalQuestions) {
                        //when the question count finished, go to the results screen
                        finishQuiz(TOTAL_TIME - MILLIS);
                    }
                    else {
                        TextView explanation = findViewById(R.id.explanation);
                        explanation.setText("");
                        resetRadioButtons();
                        questionVideo.setVisibility(View.INVISIBLE);
                        if (new Intent().getIntExtra("", 0) != 0)
                            addQuestion(specifcQuestionList);
                        else
                            addQuestion(questionList);
                    }

                    if(clicks == 1){
                        //if question was skipped the current question is flagged as 'incorrect'
                        try {
                            quizState.answerQuestion(currentQ, new UserAnswer(currentQuestionNum, false), false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    //reset the skip feature
                    clicks = 0;
                }
                else{
                    areYouSure.setText("Are you sure you want to skip?");
                    clicks++;
                }
            }
        });
    }

    /**
     * Adding a question to the question template
     *
     * Updates fields:
     *  - current question number
     *  - total questions
     *  - score
     *  - Question
     *  - all answer choices
     *  - current answer (assigns correct answer to check which radio button is correct
     */
    public void addQuestion(List<Question> questionList){
        radioGroup.clearCheck();
        currentQuestion = quizState.getCurrentQuestion(currentQuestionNum);
        categories.add(currentQuestion.getCategory());
        currentQuestionNum++;
        questionCountText.setText(currentQuestionNum + " / " + quizState.getQuestions().size());
        scoreText.setText(Integer.toString(scoreNumber));
        questionText.setText(currentQuestion.getQuestion());
        categoryText.setText(currentQuestion.getCategory());

        questionImage.setVisibility(View.VISIBLE);
        questionImage.setBackgroundResource(currentQuestion.getImage());

        //this makes sure that when the answer is checked
        //it can correctly color the correct answer and wrong answers
        correctAnswer = currentQuestion.getCorrectAnswer();
        switch(correctAnswer){
            case 1:
                correctAnswer = option1.getId();
                break;
            case 2:
                correctAnswer = option2.getId();
                break;
            case 3:
                correctAnswer = option3.getId();
                break;
            case 4:
                correctAnswer = option4.getId();
                break;
        }
        //sets all the textFields to the current question
        //questionImage.setImageBitmap(null);
        //TextView textView = findViewById(R.id.textReplacingImage);
        //textView.setText(currentQuestion.getQuestion());
        option1.setText(currentQuestion.getFirstAnswer());
        option2.setText(currentQuestion.getSecondAnswer());
        option3.setText(currentQuestion.getThirdAnswer());
        option4.setText(currentQuestion.getFourthAnswer());

    }

    private void startCountDown() {
        new CountDownTimer(MILLIS, 1000) {
            @Override
            public void onTick(long l) {
                MILLIS = (int) l;
                formatTimeView();
            }

            @Override
            public void onFinish() {
                for (; currentQuestionNum < totalQuestions; currentQuestionNum++) {
                    try {
                        quizState.answerQuestion(currentQ, new UserAnswer(currentQuestionNum, false), false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                AlertDialog dialog = new AlertDialog.Builder(zis)
                        .setMessage("Time is up! Put down your no. 2 pencils!")
                        .setPositiveButton("Show the results", (theDialog, id) -> {
                            finishQuiz(TOTAL_TIME);
                        })
                        .create();

                dialog.setTitle("Bed time! Sleepy time!");
                dialog.setIcon(R.drawable.ic_baseline_timer_off_24);
                dialog.show();
            }
        }.start();
    }

    private void finishQuiz(int timeTaken) {
        //Saves categories in a string list, since database doesnt support list feature
        String categoryList = "";
        for(String categories : categories){
            categoryList = categoryList + " " + categories;
        }
        results_db.open().finishQuiz(scoreNumber, scoreNumber, (totalQuestions - scoreNumber), categoryList);
        results_db.close();

        Intent intent = new Intent(QuizQuestionActivity.this, QuizResultActivity.class);
        intent.putExtra("Score", scoreNumber);
        intent.putExtra("Total_questions", totalQuestions);
        intent.putExtra("Time_taken", timeTaken);
        startActivity(intent);
    }

    private void formatTimeView() {
        TextView timerView = findViewById(R.id.timer);
        timerView.setTextSize(30);
        int minutes = (int) (MILLIS / 1000) / 60;
        int seconds = (int) (MILLIS / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timer.setText(timeLeftFormatted);
    }

    public void resetRadioButtons(){
        option1.setClickable(true);
        option2.setClickable(true);
        option3.setClickable(true);
        option4.setClickable(true);
        
        option1.setBackground(null);
        option1.setTypeface(null, Typeface.NORMAL);
        option2.setBackground(null);
        option2.setTypeface(null, Typeface.NORMAL);
        option3.setBackground(null);
        option3.setTypeface(null, Typeface.NORMAL);
        option4.setBackground(null);
        option4.setTypeface(null, Typeface.NORMAL);

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

    public void initializeVideoPlayer(String imageURL) {
        VideoView videoView = findViewById(R.id.videoScreen);
        String videoPath = imageURL; //question.getVideoId
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);


        MediaController controller = new MediaController(this);
        videoView.setMediaController(controller);
        controller.setAnchorView(videoView);
    }
}
