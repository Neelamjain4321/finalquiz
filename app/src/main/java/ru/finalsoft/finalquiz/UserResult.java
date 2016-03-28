package ru.finalsoft.finalquiz;

import java.util.Date;

public class UserResult {
    int result_id, result_online_id, user_id, test_id, min, current, max, pending;
    Date date;
    String test_name, description, answers;

    public UserResult(int result_id, int result_online_id, int user_id, int test_id, String
            test_name, int min, int current, int max, Date date, String description, int pending,
                      String answers) {
        this.result_id = result_id;
        this.result_online_id = result_online_id;
        this.user_id = user_id;
        this.test_id = test_id;
        this.test_name = test_name;
        this.min = min;
        this.current = current;
        this.max = max;
        this.date = date;
        this.description = description;
        this.pending = pending;
        this.answers = answers;
    }
}
