package ru.finalsoft.finalquiz;


import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class QuizAdapter extends ArrayAdapter<Quiz> {

    Context context;
    int layoutResId;
    ArrayList<Quiz> data = null;

    public QuizAdapter(Context context, int layoutResId, ArrayList<Quiz> data) {
        super(context, layoutResId, data);
        this.layoutResId = layoutResId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        QuizHolder holder;


        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(R.layout.quiz_list_item, parent, false);


            holder = new QuizHolder();
            holder.imageBackground = (ImageView) convertView.findViewById(R.id.quiz_image);
            holder.textTitle = (TextView) convertView.findViewById(R.id.quiz_name);
            holder.textDescription = (TextView) convertView.findViewById(R.id.quiz_description);
            holder.contIcon = (ImageView) convertView.findViewById(R.id.cont_icon);


            convertView.setTag(holder);
        } else {
            holder = (QuizHolder) convertView.getTag();
        }

        Quiz quiz = data.get(position);


        // holder.textScore.setText(history.score);
        holder.textTitle.setText(quiz.test_name);
        Picasso.with(this.context).load("http://ecms.mbcsoft.ru/uploads/test/" + quiz.image).into
                (holder.imageBackground);
        holder.textDescription.setText(Html.fromHtml(quiz.description));
        if (quiz.started > 0) holder.contIcon.setVisibility(View.VISIBLE);
        else holder.contIcon.setVisibility(View.GONE);

        return convertView;
    }


    static class QuizHolder {
        ImageView imageBackground;
        ImageView contIcon;
        TextView textTitle;
        TextView textDescription;
    }
}