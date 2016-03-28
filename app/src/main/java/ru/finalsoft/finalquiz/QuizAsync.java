package ru.finalsoft.finalquiz;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

class QuizAsync extends AsyncTask<Void, Void, Boolean> {
    Quiz quiz;
    String error = null;
    DBHelper databaseHelper;
    eAPI API = eAPI.getInstance();
    Callback<String> callback;
    SharedPreferences quizPref;
    int user_id;

    QuizAsync(Quiz quiz, SharedPreferences quizPref, Context context, Callback<String> callback) {
        this.quiz = quiz;
        this.callback = callback;
        this.quizPref = quizPref;
        user_id = context.getSharedPreferences("user", Context.MODE_PRIVATE).getInt("user_id", 0);
        databaseHelper = DBHelper.getInstance(context);
    }

    @Override
    protected Boolean doInBackground(Void... params) { //log in or register
        quiz.JSONImpl = null;
        try {
            quiz.JSONImpl = API.getQuiz(quiz.test_id);

            if ((quiz.started == 2) && (quiz.JSONImpl.getInt("time_limit") == 0)) {
                JSONObject old = new JSONObject(quizPref.getString("quiz_" + quiz.test_id, "{}"));
                if (old.has("answers")) {
                    System.out.println(123);
                    JSONObject phoneAnswers = old.getJSONObject("answers");
                    Iterator<String> keys = phoneAnswers.keys();

                    while (keys.hasNext()) {
                        String key = keys.next();
                        quiz.JSONImpl.put(key, phoneAnswers.getString(key));
                    }
                }

            }

            //quiz.JSONImpl.put("last_sync", new Date().getTime());
            quizPref.edit().putString("quiz_" + quiz.test_id, quiz.JSONImpl.toString()).apply();
        } catch (IOException e) {
            error = "_unable_to_resolve_host";
        } catch (JSONException e) {
            error = "_server_response_error";
        } catch (Exception e) {
            error = e.toString();
        }

        if ((error != null) && (quiz.JSONImpl == null)) {
            try {
                quiz.JSONImpl = new JSONObject(quizPref.getString("quiz_" + quiz.test_id, "{}"));
            } catch (Exception e) {
                error = e.toString();
            }
        }

        return loadQuiz(quiz.JSONImpl);
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (callback != null)
            callback.call(success ? null : error);
    }


    public boolean loadQuiz(JSONObject JSONQuiz) {
        if ((JSONQuiz == null) || !JSONQuiz.has("test_name")) return false;

        try {
            quiz.test_name = JSONQuiz.getString("test_name");
            quiz.test_id = JSONQuiz.getInt("test_id");
            quiz.time_limit = JSONQuiz.getInt("time_limit");
            quiz.category = JSONQuiz.getString("category");

            JSONObject jsonSavedAnswers = null;
            JSONObject JSONAnswers = null;
            boolean have_to_save = false;
            String postAnswers = null;

            if (JSONQuiz.has("started") && (quiz.time_limit > 0)) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                quiz.remain = (int) (df.parse(JSONQuiz.getString("started")).getTime() +
                        quiz.time_limit * 60 * 1000 - df.parse(df.format(new Date())).getTime());

                if (quiz.remain > 0)
                    quiz.remain = quiz.remain / (1000 * 60);
                else {
                    if ((quiz.started == 2) && JSONQuiz.has("answers")) {
                        String rawJSON = JSONQuiz.getJSONObject("answers").toString();
                        if (rawJSON.length() > 3) {
                            have_to_save = true;
                            JSONAnswers = new JSONObject(rawJSON);
                            postAnswers = "";
                        }
                    }
                    JSONQuiz.remove("answers");
                    JSONQuiz.put("started", df.format(new Date()));
                }

                // If remain < 0 then create new user result with result_online_id=0
                // Update: no need. when you next time will have ie connection and will open this
                // quiz the server create new result
            } else quiz.remain = quiz.time_limit;


            if ((quiz.time_limit == 0) || (quiz.remain > 0))
                try {
                    jsonSavedAnswers = JSONQuiz.has("answers") ?
                            JSONQuiz.getJSONObject("answers") : null;
                } catch (Exception e) {
                    System.out.println("JSON User answers are wrong." + e.toString());
                }
            else quiz.remain = quiz.time_limit;

            JSONArray questions = JSONQuiz.getJSONArray("questions");

            for (int i = 0; i < questions.length(); i++) {
                JSONObject jsonQuestion = questions.getJSONObject(i);
                QuizQuestion quizQuestion = new QuizQuestion(jsonQuestion.getInt("question_id"),
                        jsonQuestion.getString("question"), jsonQuestion.getString
                        ("description"), jsonQuestion.getString("tag"), jsonQuestion.getString
                        ("type").length() > 1 ? 2 : jsonQuestion.getInt
                        ("type"), jsonQuestion.getInt("default_points"),
                        jsonQuestion.getInt("posi"));
                // Поля type имеет три вида значений: 0 (одиночный); 1 (множ); 2-число (текст)
                boolean savedAnswers = false, exists = false;

                if (jsonSavedAnswers != null)
                    savedAnswers = jsonSavedAnswers.has(Integer.toString(quizQuestion.question_id));
                else if (have_to_save && (JSONAnswers != null)) {
                    exists = JSONAnswers.has(Integer.toString(quizQuestion.question_id));
                }

                if (jsonQuestion.getString("type").length() == 1) {
                    JSONArray answers = jsonQuestion.getJSONArray("answers");
                    String savedQuestionAnswers = "";

                    if (savedAnswers)
                        savedQuestionAnswers = "," + jsonSavedAnswers.getString(Integer.toString
                                (quizQuestion.question_id)) + ",";
                    else if (exists)
                        savedQuestionAnswers = "," + JSONAnswers.getString(Integer.toString
                                (quizQuestion.question_id)) + ",";

                    for (int j = 0; j < answers.length(); j++) {
                        JSONObject jsonAnswer = answers.getJSONObject(j);
                        quizQuestion.addAnswer(new QuizQuestionAnswer(jsonAnswer.getInt("answer_id"), jsonAnswer
                                .getInt("points"), jsonAnswer.getString("description"),
                                savedAnswers && savedQuestionAnswers.contains("," + j + ",")));

                        if (exists && savedQuestionAnswers.contains("," + j + ","))
                            postAnswers += "answers%5B" + quizQuestion.question_id + "%5D%5B%5D="
                                    + j + "&";
                    }
                } else if (savedAnswers)
                    quizQuestion.textAnswer = jsonSavedAnswers.getString(
                            Integer.toString(quizQuestion.question_id));
                else if (exists)
                    postAnswers += "answers%5B" + quizQuestion.question_id + "%5D%5B%5D=" +
                            URLEncoder.encode(JSONAnswers.getString(
                                    Integer.toString(quizQuestion.question_id)
                            ), "UTF-8") + "&";
                quiz.addQuestion(quizQuestion);
            }

            JSONArray resultArray = JSONQuiz.getJSONArray("results");
            for (int i = 0; i < resultArray.length(); i++) {
                JSONObject jsonResult = resultArray.getJSONObject(i);
                quiz.addResult(new QuizResult(jsonResult.getInt("score_from"), jsonResult.getInt
                        ("score_to"), jsonResult.getString("description")));
            }

            if (have_to_save && (postAnswers.length() > 6)) {
                databaseHelper.addOrUpdateUserResult(new UserResult(0, 0, user_id, quiz
                        .test_id, quiz.test_name, 0, 0, 0, new Date(), "", 1, postAnswers));
            }
            return true;
        } catch (Exception e) {
            error = e.toString();
            return false;
        }
    }

    @Override
    protected void onCancelled() {

    }
}