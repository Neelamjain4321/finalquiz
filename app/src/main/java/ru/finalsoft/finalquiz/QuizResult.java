package ru.finalsoft.finalquiz;

public class QuizResult {
    int score_from, score_to;
    String description;

    public QuizResult(int score_from, int score_to, String description) {
        this.score_from = score_from;
        this.score_to = score_to;
        this.description = description;
    }
}
