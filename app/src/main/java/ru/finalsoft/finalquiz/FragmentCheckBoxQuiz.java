package ru.finalsoft.finalquiz;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

public class FragmentCheckBoxQuiz extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        CheckBox v = new CheckBox(container.getContext());
        Bundle args = getArguments();
        v.setText(args.getString("answer"));
        v.setId(View.generateViewId());
        v.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        int m = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources()
                .getDisplayMetrics());

        params.leftMargin = m;
        params.rightMargin = m;
        params.topMargin = m;
        params.weight = 1;


        v.setLayoutParams(params);
        if (Build.VERSION.SDK_INT < 23) {
            v.setTextAppearance(container.getContext(), android.R.style.TextAppearance_Medium);
        } else {
            v.setTextAppearance(android.R.style.TextAppearance_Medium);
        }
        v.setTextColor(Color.parseColor("#404040"));

        v.setBackground(ContextCompat.getDrawable(container.getContext(), R.drawable.corners));

        m = getResources().getDimensionPixelSize(R.dimen.nav_header_quiz_margin);
        v.setPadding(10, m, 0, m);

        final int question_pos = args.getInt("question_pos");
        final int pos = args.getInt("pos");
        QuizActivity.currentQuestion.quizQuestionAnswerArrayList.get(pos).viewId = v.getId();
        final Button button = (Button) getActivity().findViewById(R.id.button);

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (QuizActivity.currentQuestion.quizQuestionAnswerArrayList.get(pos)
                        .checked ^= true) {
                    ((CheckBox) v).setTextColor(Color.parseColor("#66309f"));
                    v.setBackground(ContextCompat.getDrawable(container.getContext(), R.drawable
                            .corners_variants));
                } else {
                    ((CheckBox) v).setTextColor(Color.parseColor("#404040"));
                    v.setBackground(ContextCompat.getDrawable(container.getContext(), R.drawable.corners));
                }
                if (button != null) {
                    button.setBackground(ContextCompat
                            .getDrawable(container.getContext(), R.drawable.corners_enabled));
                    button.setTextColor(Color.parseColor("#66309f"));
                    button.setText(getString(R.string
                            .confirm));
                }
            }
        });
        if (QuizActivity.currentQuestion.quizQuestionAnswerArrayList.get(pos).checked) {
            v.setChecked(true);
            v.setTextColor(Color.parseColor("#66309f"));
            v.setBackground(ContextCompat.getDrawable(container.getContext(), R.drawable
                    .corners_variants));

            if (button != null) {
                button.setBackground(ContextCompat.getDrawable(container.getContext(), R.drawable.corners_enabled));
                button.setTextColor(Color.parseColor("#66309f"));

                if (question_pos < (QuizActivity.quiz.quizQuestionArrayList.size() - 1))
                    button.setText(getString(R.string
                            .confirm));
            }
        }

        return v;
    }
}