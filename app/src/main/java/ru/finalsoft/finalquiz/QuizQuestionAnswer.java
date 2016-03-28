package ru.finalsoft.finalquiz;

public class QuizQuestionAnswer {
    int answer_id, points, viewId;
    String description;
    boolean checked = false;

    public QuizQuestionAnswer(int answer_id, int points, String description, boolean checked) {
        this.answer_id = answer_id;
        this.points = points;
        this.description = description;
        this.checked = checked;
    }
}
