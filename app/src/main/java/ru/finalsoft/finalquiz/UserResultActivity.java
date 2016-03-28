package ru.finalsoft.finalquiz;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Date;

public class UserResultActivity extends AppCompatActivity {

    private final Quiz quiz = QuizActivity.quiz;
    String test_name, description = null;
    boolean progressDelay = true;
    private ArcProgress arcProgress;
    private int result_online_id = 0, user_id, test_id, current = 0, max = 0, min = 0, pending = 0;
    private Date date = null;
    private AsyncTask userResultPostTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_result);

        arcProgress = (ArcProgress) findViewById(R.id.arc_progress);

        if (savedInstanceState != null) {
            test_name = savedInstanceState.getString("test_name");
            user_id = savedInstanceState.getInt("user_id", 0);
            test_id = savedInstanceState.getInt("test_id", 0);
            min = savedInstanceState.getInt("min", 0);
            current = savedInstanceState.getInt("current", 0);
            max = savedInstanceState.getInt("max", 0);
            date = new Date(savedInstanceState.getLong("date", 0));
            description = savedInstanceState.getString("description");
            pending = savedInstanceState.getInt("pending", 0);
            progressDelay = false;
            initResult();
        } else if (QuizActivity.currentQuestionNo == -1) {

            Intent i = getIntent();
            test_name = i.getStringExtra("test_name");
            user_id = i.getIntExtra("user_id", 0);
            test_id = i.getIntExtra("test_id", 0);
            min = i.getIntExtra("min", 0);
            current = i.getIntExtra("current", 0);
            max = i.getIntExtra("max", 0);
            date = new Date(i.getIntExtra("date", 0));
            description = i.getStringExtra("description");
            pending = i.getIntExtra("pending", 0);
            progressDelay = false;
            initResult();
        } else {
            QuizActivity.currentQuestionNo = -1;
            test_name = quiz.test_name;
            test_id = quiz.test_id;

            date = new Date();

            user_id = getSharedPreferences("user", MODE_PRIVATE).getInt("user_id", 0);

            if (pending > 0) {
                description = getString(R.string.result_is_pending);
                current = 0;
            }

            userResultPostTask = new UserResultPostTask(new Callback<String>() {
                @Override
                public void call(String answers) {
                    DBHelper.getInstance(UserResultActivity.this).addOrUpdateUserResult(new UserResult(
                            0, result_online_id, user_id, test_id, test_name, min, current, max,
                            date, description, pending, answers
                    ));
                    UserResultActivity.this.initResult();
                }
            }).execute();

            DBHelper.getInstance(this).setQuizzesStartedByIds("0", String.valueOf(test_id));

            quiz.JSONImpl.remove("started");
            quiz.JSONImpl.remove("answers");
            getSharedPreferences("quizzes", MODE_PRIVATE).edit()
                    .putString("quiz_" + quiz.test_id, quiz.JSONImpl.toString())
                    .commit();
        }

    }

    private void initResult() {
        final int total = current + Math.abs(min);
        final int from = max + Math.abs(min);

        View view = findViewById(R.id.progress);
        if (view != null)
            view.setVisibility(View.GONE);

        if ((view = findViewById(R.id.scroll_quiz_form)) != null)
            view.setVisibility(View.VISIBLE);

        final TextView resultTextView = (TextView) findViewById(R.id.result_text);
        if ((resultTextView != null) && (description != null)) {
            resultTextView.setText(description);
            if (progressDelay)
                resultTextView.setHeight(1);
        }


        //progress bar
        if (progressDelay && (total > 0)) {
            new CountDownTimer(2200, 2200 / (from > 0 ? ((int) (total / (from /
                    100.0))) :
                    100)) {
                public void onTick(long millisUntilFinished) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            arcProgress.setProgress(arcProgress.getProgress() + 1);
                        }
                    });
                }

                public void onFinish() {
                    arcProgress.setProgress(from > 0 ? ((int) (total / (from / 100.0))) : 100);
                    if (resultTextView != null)
                        resultTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                expand(resultTextView, resultTextView.getLineHeight() * resultTextView
                                        .getLineCount() + 12);
                            }
                        });
                }

            }.start();
        } else {
            if (arcProgress != null)
                arcProgress.setProgress(from > 0 ? ((int) (total / (from / 100.0))) : 100);
            if (progressDelay && (resultTextView != null)) {
                resultTextView.post(new Runnable() {
                    @Override
                    public void run() {
                        expand(resultTextView, resultTextView.getLineHeight() * resultTextView
                                .getLineCount());
                    }
                });
            }

        }

        //title
        test_name = test_name.toLowerCase();

        String title = (test_name.contains(getString(R.string.quiz)) ? test_name.replace
                ("\\." + "(?=[^.]*$)", "") : (getString(R.string.quiz) + " \"" + test_name
                + "\"")) + " " + getString(R.string.quiz_is_passed);

        TextView result_title = (TextView) findViewById(R.id.result_title);
        if (result_title != null)
            result_title.setText(title);
    }

    @Override
    public void onBackPressed() {
        QuizActivity.quiz = null;
        super.onBackPressed();
    }


    public void expand(final View v, final int targetHeight) {
        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.setVisibility(View.VISIBLE);

        v.getLayoutParams().height = 1;

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? targetHeight
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(1000);
        v.startAnimation(a);
    }

    public void processAnswers() {
        for (QuizQuestion q : quiz.quizQuestionArrayList) {
            int points = q.default_points;
            boolean was_checked = false;
            if (q.type == 0) {
                int max_value = Integer.MIN_VALUE;
                int min_value = Integer.MAX_VALUE;
                for (QuizQuestionAnswer a : q.quizQuestionAnswerArrayList) {

                    if (a.checked) points = a.points;
                    if (a.points > max_value) max_value = a.points;
                    if (a.points < min_value) min_value = a.points;
                }
                current += points;
                max += Math.max(max_value, q.default_points);
                min += Math.min(min_value, q.default_points);
            } else if (q.type == 1) {
                int max_value = 0;
                int min_value = 0;
                for (QuizQuestionAnswer a : q.quizQuestionAnswerArrayList) {
                    if (a.checked)
                        if (was_checked) {
                            points += a.points;
                        } else {
                            was_checked = true;
                            points = a.points;
                        }

                    if (a.points > 0) max_value += a.points;
                    else min_value += a.points;

                }
                current += points;
                max += Math.max(max_value, q.default_points);
                min += Math.min(min_value, q.default_points);
            } else {
                if (q.textAnswer.trim().equals("")) {
                    pending = 0;
                    current += points;
                    if (points > 0) max += points;
                    else min += points;
                } else pending = 1;
            }
        }
    }

    public String findResultDescription(int current) {
        for (QuizResult r : quiz.quizResultArrayList)
            if ((r.score_from <= current) && (r.score_to >= current))
                return r.description;
        return null;
    }

    public void btnTryAgain(View v) {
        Intent intent = new Intent(getBaseContext(), QuizActivity.class);
        intent.putExtra("test_id", test_id);
        intent.putExtra("test_name", test_name);
        startActivity(intent);
        finish();
    }

    public void btnShare(View v) {
        int from = current + Math.abs(min);
        int total = max + Math.abs(min);
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String textToSend = getString(R.string.text_share, from > 0 ?
                ((int) (total / (from / 100.0)) / 100) : 10, 10, quiz.test_name);
        intent.putExtra(Intent.EXTRA_TEXT, textToSend);
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_result)));
        } catch (android.content.ActivityNotFoundException ex) {
            System.out.println(ex.toString());
        }
    }

    @Override
    protected void onPause() {

        if (userResultPostTask != null)
            userResultPostTask.cancel(true);

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current state
        savedInstanceState.putInt("user_id", user_id);
        savedInstanceState.putInt("test_id", test_id);
        savedInstanceState.putString("test_name", test_name);
        savedInstanceState.putInt("min", min);
        savedInstanceState.putInt("current", current);
        savedInstanceState.putInt("max", max);
        savedInstanceState.putLong("date", date.getTime());
        savedInstanceState.putInt("pending", pending);
        savedInstanceState.putString("description", description);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    class UserResultPostTask extends AsyncTask<Void, Void, Void> {
        Callback<String> callback;
        String answers = "";

        UserResultPostTask(Callback<String> callback) {
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                for (QuizQuestion q : quiz.quizQuestionArrayList)
                    if (q.type == 2)
                        answers += "answers%5B" + q.question_id + "%5D%5B%5D=" +
                                URLEncoder.encode(q.textAnswer, "UTF-8") + "&";
                    else if (q.type == 1) {
                        for (int i = 0; i < q.quizQuestionAnswerArrayList.size(); i++)
                            if (q.quizQuestionAnswerArrayList.get(i).checked)
                                answers += "answers%5B" + q.question_id + "%5D%5B%5D=" + i + "&";
                    } else
                        for (int i = 0; i < q.quizQuestionAnswerArrayList.size(); i++)
                            if (q.quizQuestionAnswerArrayList.get(i).checked) {
                                answers += "answers%5B" + q.question_id + "%5D%5B%5D=" + i + "&";
                                break;
                            }
                JSONObject jsonUserResult = eAPI.getInstance().postUserResult(quiz.test_id, answers);
                result_online_id = jsonUserResult.getInt("id");
                min = jsonUserResult.getInt("min");
                current = jsonUserResult.getInt("current");
                max = jsonUserResult.getInt("max");
                description = jsonUserResult.getString("description");
                pending = jsonUserResult.getInt("pending");
            } catch (Exception e) {
                processAnswers();
                description = findResultDescription(current);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            callback.call(answers);
        }
    }
}
