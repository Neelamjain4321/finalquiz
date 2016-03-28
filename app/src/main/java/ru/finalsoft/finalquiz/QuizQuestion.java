package ru.finalsoft.finalquiz;

import java.util.ArrayList;

public class QuizQuestion {
    int question_id;
    String question, description, tag, textAnswer;
    int type = 0, default_points = 0, posi = 0;
    ArrayList<QuizQuestionAnswer> quizQuestionAnswerArrayList;


    public QuizQuestion(int question_id, String question, String description, String tag, int type, int default_points, int posi) {
        this.question_id = question_id;
        this.question = question;
        this.description = description;
        this.tag = tag;
        this.type = type;
        this.default_points = default_points;
        this.posi = posi;
        this.quizQuestionAnswerArrayList = new ArrayList<>();
    }

    public boolean addAnswer(QuizQuestionAnswer quizQuestionAnswer) {
        return quizQuestionAnswerArrayList.add(quizQuestionAnswer);
    }

    public void setAnswer(String textAnswer) {
        this.textAnswer = textAnswer;
    }
}
