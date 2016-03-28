package ru.finalsoft.finalquiz;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class Quiz implements Serializable {
    int test_id, time_limit;
    float remain;
    int started;
    String test_name, image, description, category;
    ArrayList<QuizQuestion> quizQuestionArrayList;
    ArrayList<QuizResult> quizResultArrayList;
    JSONObject JSONImpl;

    public Quiz(int test_id, String test_name, String image, String description,
                String category) {
        this.test_id = test_id;
        this.test_name = test_name;
        this.image = image;
        this.description = description;
        this.category = category;
    }

    public Quiz(int test_id, int time_limit, String test_name) {
        this.test_id = test_id;
        this.test_name = test_name;
        this.time_limit = time_limit;
        quizQuestionArrayList = new ArrayList<>();
        quizResultArrayList = new ArrayList<>();
    }

    public boolean addQuestion(QuizQuestion quizQuestion) {
        return quizQuestionArrayList.add(quizQuestion);
    }

    public boolean addResult(QuizResult quizResult) {
        return quizResultArrayList.add(quizResult);
    }
}
