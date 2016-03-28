package ru.finalsoft.finalquiz;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;

import org.json.JSONObject;

import java.util.List;

public class QuizActivity extends FragmentActivity {
    static Quiz quiz = null;
    static int currentQuestionNo = -1;
    static boolean saved = false;
    static QuizQuestion currentQuestion = null;
    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;
    EditText textAnswer;
    DonutProgress timerProgress;
    TextView timerText, questionNoText;
    boolean finished = false;
    AsyncTask quizLoadingTask = null;
    CountDownTimer cdtimer = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_quiz);
        fragmentManager = getSupportFragmentManager();
        textAnswer = (EditText) findViewById(R.id.textAnswer);
        timerProgress = (DonutProgress) findViewById(R.id.donut_progress);
        timerText = (TextView) findViewById(R.id.timerText);
        questionNoText = (TextView) findViewById(R.id.question_no);

        if (saved) {
            currentQuestionNo--;
            findViewById(R.id.progress).setVisibility(View.GONE);
            findViewById(R.id.scroll_quiz_form).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.total_questions)).setText("/ " + quiz.quizQuestionArrayList.size());
            nextQuestionBtn(null);
        } else {
            initQuiz();
        }

    }

    protected void initQuiz() {
        final SharedPreferences quizPref = getSharedPreferences("quizzes", MODE_PRIVATE);

        final Animation myAnim = AnimationUtils.loadAnimation(this, R.anim.shakebtn);

        currentQuestionNo = -1;

        Intent i = getIntent();
        quiz = new Quiz(i.getIntExtra("test_id", 0), 0, i.getStringExtra("test_name"));
        quiz.started = i.getIntExtra("started", 0);

        quizLoadingTask = new QuizAsync(quiz, quizPref, getBaseContext(), new Callback<String>() {
            @Override
            public void call(String error) {
                findViewById(R.id.progress).setVisibility(View.GONE);

                if (error == null) {
                    findViewById(R.id.scroll_quiz_form).setVisibility(View.VISIBLE);
                    findViewById(R.id.quiz_question).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.total_questions))
                            .setText("/ " + quiz.quizQuestionArrayList.size());
                    final Button myButton = (Button) findViewById(R.id.button);
                    myButton.setAnimation(myAnim);
                    myButton.startAnimation(myAnim);
                    nextQuestionBtn(null);

                    DBHelper.getInstance(getBaseContext())
                            .setQuizzesStartedByIds(String.valueOf(quiz.started), String.valueOf(quiz.test_id));


                    textAnswer.addTextChangedListener(new TextWatcher() {
                        public void afterTextChanged(Editable s) {
                        }

                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            quiz.quizQuestionArrayList.get(currentQuestionNo).setAnswer(textAnswer.getText().toString());
                        }
                    });

                    if (quiz.time_limit > 0) {

                        findViewById(R.id.progress_layout).setVisibility(View.VISIBLE);
                        final int quizTime = (int) (quiz.remain * 60 * 1000);
                        final int fromTime = quiz.time_limit * 60 * 1000;
                        cdtimer = new CountDownTimer(quizTime, 1000) {
                            public void onTick(final long millisUntilFinished) {

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        long hours = millisUntilFinished / 1000 / 60 / 60;
                                        long minutes = (millisUntilFinished / 1000 / 60) % 60;
                                        long seconds = (millisUntilFinished / 1000) % 60;

                                        timerProgress.setProgress((int) (100 -
                                                (millisUntilFinished * 100.0) / fromTime));

                                        timerText.setText(
                                                ((hours > 0) ? (hours < 10 ? "0" : "") + hours +
                                                        ":" : "") +
                                                        (minutes < 10 ? "0" : "") + Long.toString(minutes) +
                                                        ":" +
                                                        (seconds < 10 ? "0" : "") + Long.toString(seconds)
                                        );
                                    }
                                });
                            }

                            public void onFinish() {
                                startActivity(new Intent(getBaseContext(), UserResultActivity.class));
                                finish(); //завершаем текущее
                            }
                        }.start();
                    }
                } else {
                    Class c = R.string.class;
                    findViewById(R.id.errorLayout).setVisibility(View.VISIBLE);


                    try {
                        ((TextView) findViewById(R.id.errorTitle)).setText(getString((int) c.getField(error).get(c)));
                    } catch (Exception e) {
                        ((TextView) findViewById(R.id.errorTitle)).setText(getString(R.string.error) + ": " + error);
                    }
                }
            }
        }).execute((Void) null);

    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current state
        saved = currentQuestionNo >= 0;
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onPause() {
        //if quiz isn't finished we will save answers
        if (!finished && (quiz.JSONImpl != null))
            getSharedPreferences("quizzes", MODE_PRIVATE).edit()
                    .putString("quiz_" + quiz.test_id, quiz.JSONImpl.toString()).commit();

        if (cdtimer != null)
            cdtimer.cancel();

        if (quizLoadingTask != null)
            quizLoadingTask.cancel(true);

        super.onPause();
    }

    public void nextQuestionBtn(View v) {
        if ((quiz.quizQuestionArrayList.size() - 1) == currentQuestionNo) {
            //ProgressDialog.show(this, null, getString(R.string.request_processing), true);
            finished = true;
            startActivity(new Intent(getBaseContext(), UserResultActivity.class));
            finish(); //завершаем текущее
            return;
        }

        if ((quiz.started == 0) && ((quiz.time_limit > 0) || (currentQuestionNo >= 0))) {
            quiz.started = 2;
            DBHelper.getInstance(getBaseContext())
                    .setQuizzesStartedByIds("2", String.valueOf(quiz.test_id));
        }

        if (currentQuestion != null) {
            new SaveUserAnswerAsync(quiz.test_id, currentQuestion).execute();

            //
            String strAnswers = "";
            if (currentQuestion.type < 2) {
                int i = 0;
                for (QuizQuestionAnswer a : currentQuestion.quizQuestionAnswerArrayList) {
                    if (a.checked) strAnswers += i + ",";
                    i++;
                }
                if (strAnswers.length() > 0) //remove last ","
                    strAnswers = strAnswers.substring(0, strAnswers.length() - 1);
            } else strAnswers = textAnswer.getText().toString();
            try {

                if (quiz.JSONImpl.has("answers") && !quiz.JSONImpl.getString("answers").equals("[]")) {
                    if (strAnswers.length() > 0) {
                        quiz.JSONImpl.getJSONObject("answers").put(
                                Integer.toString(currentQuestion.question_id), strAnswers);
                    } else {
                        quiz.JSONImpl.getJSONObject("answers").remove(Integer.toString
                                (currentQuestion.question_id));
                    }
                } else if (strAnswers.length() > 0)
                    quiz.JSONImpl.put("answers",
                            new JSONObject().put(Integer.toString(currentQuestion.question_id), strAnswers));
            } catch (Exception e) {
                System.out.println("can't save answers in nextQuestionBtn " + e.toString());
            }
        }

        currentQuestionNo++;
        initQuestion();
    }

    private void initQuestion() {
        currentQuestion = quiz.quizQuestionArrayList.get(currentQuestionNo);

        questionNoText.setText(" " + (currentQuestionNo + 1));

        ((TextView) findViewById(R.id.quiz_question)).setText(currentQuestion.question);
        textAnswer.setVisibility(View.GONE);

        List<Fragment> al = fragmentManager.getFragments();
        fragmentTransaction = fragmentManager.beginTransaction();

        if (al != null)
            for (Fragment frag : al) {
                if (frag != null) {
                    fragmentTransaction.remove(frag);
                }
            }
        int i = 0;
        if (currentQuestion.type == 0) {
            for (QuizQuestionAnswer quizQuestionAnswer : currentQuestion.quizQuestionAnswerArrayList) {
                FragmentRadioQuiz f = new FragmentRadioQuiz();
                Bundle args = new Bundle();
                args.putString("answer", quizQuestionAnswer.description);
                args.putInt("question_pos", currentQuestionNo);
                args.putInt("pos", i++);
                f.setArguments(args);
                fragmentTransaction.add(R.id.radio_container, f, "ANSWERS");
            }
        } else if (currentQuestion.type == 1) {
            for (QuizQuestionAnswer quizQuestionAnswer : currentQuestion.quizQuestionAnswerArrayList) {
                FragmentCheckBoxQuiz f = new FragmentCheckBoxQuiz();
                Bundle args = new Bundle();
                args.putString("answer", quizQuestionAnswer.description);
                args.putInt("question_pos", currentQuestionNo);
                args.putInt("pos", i++);
                f.setArguments(args);
                fragmentTransaction.add(R.id.checkbox_container, f, "ANSWERS");
            }
        } else {
            textAnswer.setText("");
            textAnswer.setVisibility(View.VISIBLE);
            if (textAnswer.requestFocus()) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        }
        fragmentTransaction.commit();

        findViewById(R.id.button).setBackground(ContextCompat.getDrawable(this, R.drawable.corners));
        ((Button) findViewById(R.id.button)).setTextColor(Color.parseColor("#606060"));

        if ((quiz.quizQuestionArrayList.size() - 1) == currentQuestionNo)
            ((Button) findViewById(R.id.button)).setText(getString(R.string.quiz_finish));
        else if (currentQuestion.type != 2)
            ((Button) findViewById(R.id.button)).setText(getString(R.string.quiz_continue));
        else ((Button) findViewById(R.id.button)).setTextColor(Color.parseColor("#000000"));


    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.want_exit))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        saved = false;
                        currentQuestionNo = -1;
                        finish();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    public void btnUpdate(View v) {
        findViewById(R.id.errorLayout).setVisibility(View.GONE);
        initQuiz();
    }

    class SaveUserAnswerAsync extends AsyncTask<Void, Void, Void> {
        int test_id;
        QuizQuestion currentQuestion;

        SaveUserAnswerAsync(int test_id, QuizQuestion currentQuestion) {
            this.test_id = test_id;
            this.currentQuestion = currentQuestion;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                eAPI.getInstance().postUserAnswer(test_id, currentQuestion);
                if (quiz.started != 1) {
                    quiz.started = 1;
                    DBHelper.getInstance(getBaseContext())
                            .setQuizzesStartedByIds("1", String.valueOf(quiz.test_id));
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            }
            return null;
        }

    }

}
