package ru.finalsoft.finalquiz;


import android.app.Activity;
import android.content.Context;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class UserResultListAdapter extends ArrayAdapter<UserResult> {

    static DateFormat df = new SimpleDateFormat("dd/MMM/yyyy HH:mm", Locale.getDefault());
    Context context;
    int layoutResId;
    ArrayList<UserResult> userResultList = null;

    public UserResultListAdapter(Context context, int layoutResId, ArrayList<UserResult> userResultList) {
        super(context, layoutResId, userResultList);
        this.layoutResId = layoutResId;
        this.context = context;
        this.userResultList = userResultList;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        UserResultHolder holder;


        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(R.layout.result_list_item, parent, false);


            holder = new UserResultHolder();
            holder.textTitle = (TextView) convertView.findViewById(R.id.title);
            holder.textDescription = (TextView) convertView.findViewById(R.id.description);
            convertView.setTag(holder);
        } else {
            holder = (UserResultHolder) convertView.getTag();
        }


        UserResult userResult = userResultList.get(position);

        if (userResult.pending != 0) {
            convertView.setBackground(ResourcesCompat.getDrawable(convertView.getResources(),
                    R.drawable.drawable_disabled, null));
        } else
            convertView.setBackground(ResourcesCompat.getDrawable(convertView.getResources(),
                    R.drawable.drawable_transparent, null));

        // holder.textScore.setText(history.score);
        holder.textTitle.setText(userResult.test_name);

        holder.textDescription.setText(df.format(userResult.date));


        return convertView;
    }

    public boolean areAllItemsEnabled() {
        return false;
    }

    public boolean isEnabled(int position) {
        return userResultList.get(position).pending == 0;
    }

    static class UserResultHolder {
        TextView textTitle;
        TextView textDescription;
    }
}