package ru.finalsoft.finalquiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    ArrayAdapter<Quiz> QuizAdapter;
    ArrayAdapter<UserResult> UserResultAdapter;
    LinkedHashMap<String, String> cats = new LinkedHashMap<>();
    int page = 1, maxpage = 1;
    private eAPI API = eAPI.getInstance();
    private DBHelper databaseHelper;
    private SharedPreferences loginPref, appPref;
    private MainAsyncTask mMainTask = null;
    private int anim;
    private RotateAnimation rotateAnim;
    private String current_category = "all";
    private Spinner spinner;
    private ListView quizzesView, resultsView;
    private TextView mText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loginPref = getSharedPreferences("user", MODE_PRIVATE);
        appPref = getSharedPreferences("app", MODE_PRIVATE);
        databaseHelper = DBHelper.getInstance(getBaseContext());
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        if (getSupportActionBar() != null) //отключаем title
            getSupportActionBar().setDisplayShowTitleEnabled(false);


        //устанавливаем тесты
        quizzesView = (ListView) findViewById(R.id.quizzes_container);
        if (quizzesView != null) {
            quizzesView.setClickable(true);
            quizzesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getBaseContext(), QuizActivity.class);
                    intent.putExtra("test_id", QuizAdapter.getItem(position).test_id);
                    intent.putExtra("test_name", QuizAdapter.getItem(position).test_name);
                    intent.putExtra("started", QuizAdapter.getItem(position).started);
                    startActivity(intent);
                    // показываем новое Activity
                }
            });
        }

        resultsView = (ListView) findViewById(R.id.results_container);
        if (resultsView != null) {
            resultsView.setClickable(true);
            resultsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    UserResult userResult = UserResultAdapter.getItem(position);

                    Intent intent = new Intent(getBaseContext(), UserResultActivity.class);

                    intent.putExtra("result_online_id", userResult.result_online_id);
                    intent.putExtra("user_id", userResult.user_id);
                    intent.putExtra("test_id", userResult.test_id);
                    intent.putExtra("test_name", userResult.test_name);
                    intent.putExtra("min", userResult.test_name);
                    intent.putExtra("current", userResult.current);
                    intent.putExtra("max", userResult.max);
                    intent.putExtra("date", userResult.date.getTime());
                    intent.putExtra("description", userResult.description);
                    intent.putExtra("pending", userResult.pending);
                    startActivity(intent);
                    // показываем новое Activity
                }
            });
        }


        //устанавливаем категории
        try {
            cats.put(getString(R.string.all_quizzes), "all");
            JSONArray jsonArray = new JSONArray(appPref.getString("cats", "[]"));

            for (int i = 0; i < jsonArray.length(); i++)
                cats.put(jsonArray.getJSONArray(i).getString(1), jsonArray.getJSONArray(i)
                        .getString(0));
        } catch (JSONException e) {
            System.out.println("Cats from pref are wrong: " + e);
        }

        initSpinner(new ArrayList<>(cats.keySet()), true);
        // </категории

        initQuizzes(1);
        sync(false);

        //главная меню
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null)
            drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null)
            navigationView.setNavigationItemSelectedListener(this);

        quizzesView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

                int lastInScreen = firstVisibleItem + visibleItemCount + 7;

                if ((lastInScreen == totalItemCount) && (page <= maxpage) && (anim <= 1)) {
                    mMainTask = new QuizzesListAsyncTask();
                    mMainTask.execute(++page);
                }
            }
        });
        resultsView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

                int lastInScreen = firstVisibleItem + visibleItemCount + 40;

                if ((lastInScreen == totalItemCount) && (page <= maxpage) && (anim <= 1)) {
                    mMainTask = new ResultListAsyncTask();
                    mMainTask.execute(++page);
                }
            }
        });


    }

    private void sync(final boolean force) {

        if (anim > 1 && mMainTask != null) return;

        mText = (TextView) findViewById(R.id.action_sync_btn);

        float fromRotation = (mText != null) ? mText.getRotation() % 360 : 0;
        float toRotation = fromRotation + 360;
        rotateAnim = new RotateAnimation(
                fromRotation, toRotation, (mText != null) ? mText.getWidth() / 2 : 50, (mText != null) ? mText
                .getHeight() / 2 : 50);
        rotateAnim.setDuration(1000); // Use 0 ms to rotate instantly
        rotateAnim.setRepeatCount(Animation.INFINITE);
        rotateAnim.setFillAfter(false); // Must be true or the animation will reset
        mText.startAnimation(rotateAnim);

        mText = (TextView) findViewById(R.id.action_sync_text);
        setText(mText, getString(R.string.sync_started));

        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        anim = 1;

        try {
            new QuizzesUnfinishedAsyncTask().execute();
            if (force || (appPref.getLong("quizzes_last_sync", 0) < calendar.getTime().getTime())) {
                QuizAdapter = null;
                page = 1;
                initQuizzes(1);
                new QuizzesListAsyncTask().execute();
                // TODO В будущем нужно добавить разрешение конфлика
                // Если у пользователя исчез доступ к тесту, то нужно удалить его из приложения
                // При синхронизации с интернетом можно удалять первую страницу тестов из бд,
                // а затем добавлять из интернета. Когда дойдем до последней, очистим все остальное

            }
            if (force || (appPref.getLong("cats_last_sync", 0) < calendar.getTime().getTime())) {
                new CategoriesAsyncTask().execute();
            }

            new SendUnsavedUserResultsAsyncTask().execute();



            UserResultAdapter = null;
            page = 1;
            initResults(1);
            mMainTask = new ResultListAsyncTask();
            mMainTask.execute();

        } catch (Exception e) {
            System.out.println("sync wrong:" + e);
        }
    }

    @Override
    protected void onResume() {
        sync(false);
        super.onResume();
    }

    private void stopSyncAnim() {
        anim--;
        if (anim <= 1) {
            mMainTask = null;
            rotateAnim.cancel();
            rotateAnim.reset();
            DateFormat df = new SimpleDateFormat("d MMM HH:mm", Locale.getDefault());
            setText((TextView) findViewById(R.id.action_sync_text), getString(R.string.last_sync)
                    + ((anim < -12) ? (" " + getString(R.string.bad_sync) + ": " + df.format(new
                    Date())) :
                    (":\n" + df.format(Math.min(appPref.getLong("cats_last_sync", 0),
                            Math.min(appPref.getLong("quizzes_last_sync", 0), appPref.getLong
                                    ("results_last_sync", 0)))))
            ));
        }
    }


    private void initSpinner(List<String> set, final boolean restart) {
        spinner = (Spinner) findViewById(R.id.spinner);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(), R.layout.spinner_items, set);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        final int pos = spinner.getSelectedItemPosition();


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setAdapter(adapter);
                if (restart || (pos == -1)) {
                    ViewTreeObserver vto = spinner.getViewTreeObserver();
                    vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            spinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            int width = spinner.getMeasuredWidth();
                            Spinner.LayoutParams lp = spinner.getLayoutParams();
                            lp.width = width + 70; //добавляем ширину, чтобы поместилась стрелка
                            spinner.setLayoutParams(lp);
                        }
                    });
                } else spinner.setSelection(pos);

                if (spinner.getOnItemClickListener() == null)
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            current_category = cats.get(spinner.getSelectedItem().toString());
                            QuizAdapter = null;
                            page = 1;
                            initQuizzes(1);
                            mMainTask = new QuizzesListAsyncTask();
                            mMainTask.execute();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                        }

                    });
            }
        });
    }

    private void initQuizzes(final int page) { //записывает тесты в категорию
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (QuizAdapter == null) {
                    QuizAdapter = new QuizAdapter(MainActivity.this,
                            R.layout.quiz_list_item,
                            (ArrayList<Quiz>) databaseHelper.getQuizList(current_category, page));
                    quizzesView.setAdapter(QuizAdapter);
                } else {
                    QuizAdapter.addAll(databaseHelper.getQuizList(current_category, page));
                    QuizAdapter.notifyDataSetChanged();
                }

            }
        });
    }

    private void initResults(final int page) { //записывает тесты в категорию
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (UserResultAdapter == null) {
                    UserResultAdapter = new UserResultListAdapter(MainActivity.this,
                            R.layout.result_list_item,
                            (ArrayList<UserResult>) databaseHelper.getUserResultList(loginPref.getInt("user_id", 0), page));
                    resultsView.setAdapter(UserResultAdapter);
                } else {
                    UserResultAdapter.addAll(databaseHelper.getUserResultList(loginPref.getInt("user_id", 0), page));
                    UserResultAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if ((drawer != null) && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);

        try {
            mText = (TextView) findViewById(R.id.header_name);
            setText(mText, loginPref.getString("first_name", loginPref.getString("user_name", ""))
                    + " " + loginPref.getString("last_name", ""));

            mText = (TextView) findViewById(R.id.header_email);
            setText(mText, loginPref.getString("user_email", ""));

            mText = (TextView) findViewById(R.id.action_sync_text); //sync text

            DateFormat df = new SimpleDateFormat("d MMM HH:mm", Locale.getDefault());

            setText(mText, getString(R.string.last_sync) + ":\n" + df.format(appPref.getLong("cats_last_sync", 0)));

            mText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sync(true);
                }
            });
            mText = (TextView) findViewById(R.id.action_sync_btn);
            mText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sync(true);

                }
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return true;
    }

    private void setText(TextView mText, String text) {
        try {
            mText.setVisibility(View.GONE);
            mText.setText(text);
            mText.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_quizzes) {

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.setDisplayShowTitleEnabled(false);
            spinner.setVisibility(View.VISIBLE);
            spinner.setSelection(0);
            quizzesView.setVisibility(View.VISIBLE);
            resultsView.setVisibility(View.GONE);
            page = maxpage = 1;
            QuizAdapter = null;
            initQuizzes(1);

        } else if (id == R.id.nav_random_quiz) {

            Random randomGenerator = new Random();
            int position = randomGenerator.nextInt(QuizAdapter.getCount());
            Intent intent = new Intent(getBaseContext(), QuizActivity.class);
            intent.putExtra("test_id", QuizAdapter.getItem(position).test_id);
            intent.putExtra("test_name", QuizAdapter.getItem(position).test_name);
            intent.putExtra("started", QuizAdapter.getItem(position).started);
            startActivity(intent);

        } else if (id == R.id.nav_results) {

            Spinner spinner = (Spinner) findViewById(R.id.spinner);
            if (spinner != null)
                spinner.setVisibility(View.GONE);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle(getString(R.string.title_my_results));
            }
            quizzesView.setVisibility(View.GONE);
            resultsView.setVisibility(View.VISIBLE);
            page = maxpage = 1;
            UserResultAdapter = null;
            initResults(1);

        } else if (id == R.id.nav_settings) {

            startActivity(new Intent(getBaseContext(), SettingsActivity.class));

        }/* else if (id == R.id.nav_logout) {

            databaseHelper.removeSavedUserResults(loginPref.getInt("user_id", 0));
            databaseHelper.removeQuizzes();
            String email = loginPref.getString("user_email", "");
            loginPref.edit().clear().putString("user_email", email).apply();
            API.setCookie(null);
            appPref.edit().clear().apply();  //удаляем данные о тестах, синхронизациях и тд.
            startActivity(new Intent(getBaseContext(), LoginActivity.class));
            finish(); //завершаем текущее
            return true;

        }
        */

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null)
            drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    abstract class MainAsyncTask extends AsyncTask<Integer, Void, Boolean> {

        MainAsyncTask() {
            anim++;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (!success)
                anim -= 5;

            stopSyncAnim(); //выключаем анимацию
        }

        @Override
        protected void onCancelled() {
            stopSyncAnim();
        }
    }

    public class CategoriesAsyncTask extends MainAsyncTask {

        @Override
        protected Boolean doInBackground(Integer... params) { //log in or register
            SharedPreferences.Editor editor = appPref.edit();
            try {
                JSONArray jsonArray = API.getCats();
                editor.putLong("cats_last_sync", new Date().getTime());

                if (!appPref.getString("cats", "[]").equals(jsonArray.toString())) {
                    cats = new LinkedHashMap<>();
                    cats.put(getString(R.string.all_quizzes), "all");
                    for (int i = 0; i < jsonArray.length(); i++)
                        cats.put(jsonArray.getJSONArray(i).getString(1), jsonArray
                                .getJSONArray(i).getString(0));

                    initSpinner(new ArrayList<>(cats.keySet()), true);

                    editor.putString("cats", jsonArray.toString());
                }
            } catch (Exception e) {
                System.out.println("in doin cats: " + e.toString());
                return false;
            }

            editor.apply();

            return true;
        }
    }

    public class SendUnsavedUserResultsAsyncTask extends MainAsyncTask {

        @Override
        protected Boolean doInBackground(Integer... params) {
            try {
                List<UserResult> userResultList = databaseHelper.getUnsavedUserResultList(
                        loginPref.getInt("user_id", 0), 1);
                if (userResultList.size() > 0) {
                    for (UserResult userResult : userResultList) {
                        JSONObject jsonUserResult = API.postUserResult
                                (userResult.test_id, userResult.answers);
                        userResult.result_online_id = jsonUserResult.getInt("id");
                        userResult.min = jsonUserResult.getInt("min");
                        userResult.current = jsonUserResult.getInt("current");
                        userResult.max = jsonUserResult.getInt("max");
                        userResult.description = jsonUserResult.getString("description");
                        userResult.pending = jsonUserResult.getInt("pending");
                        databaseHelper.addOrUpdateUserResult(userResult);
                    }

                    UserResultAdapter = null;
                    initResults(1);
                    //here we could update only unsaved answers
                }
            } catch (Exception e) {
                System.out.println("in doin post unsaved user results: " + e.toString());
                return false;
            }

            return true;
        }
    }

    public class ResultListAsyncTask extends MainAsyncTask {

        @Override
        protected Boolean doInBackground(Integer... params) {
            SharedPreferences.Editor editor = appPref.edit();
            boolean changes = false;
            //System.out.println(sync_method + " to page " + page);

            try {
                JSONObject jsonAPIUserResults = API.getUserResults(page);
                maxpage = (int) Math.ceil(jsonAPIUserResults.getInt("count") / 50.0);
                editor.putLong("results_last_sync", new Date().getTime());

                if (maxpage > 0) {
                    JSONArray jsonArray = jsonAPIUserResults.getJSONArray("scores");

                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    df.setTimeZone(TimeZone.getTimeZone("UTC"));

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonUserResult = jsonArray.getJSONObject(i);
                        if ((databaseHelper.addOrUpdateUserResult(
                                new UserResult(0,
                                        jsonUserResult.getInt("result_id"),
                                        jsonUserResult.getInt("user_id"),
                                        jsonUserResult.getInt("test_id"),
                                        jsonUserResult.getString("test_name"),
                                        jsonUserResult.getInt("min"),
                                        jsonUserResult.getInt("current"),
                                        jsonUserResult.getInt("max"),
                                        df.parse(jsonUserResult.getString("date")),
                                        jsonUserResult.getString("description"),
                                        jsonUserResult.getInt("pending"), "")) > 0) &&
                                (page == 1))
                            changes = true;
                    }
                }


            } catch (Exception e) {
                System.out.println("in doin user_results: " + e.toString());
                if (page >= maxpage)
                    maxpage = (int) Math.ceil(databaseHelper.getUserResultsCount(loginPref
                            .getInt("user_id", 0)) / 50.0);
                return false;
            } finally {
                if (changes) {
                    UserResultAdapter = null;
                    initResults(page);
                } else if (page > 1)
                    initResults(page); //если изменения в бд из
                // стр.1 или стр. != 1
            }
            editor.apply();

            return true;
        }
    }

    public class QuizzesListAsyncTask extends MainAsyncTask {

        @Override
        protected Boolean doInBackground(Integer... params) {
            SharedPreferences.Editor editor = appPref.edit();
            boolean changes = false;

            try {
                JSONObject jsonAPIQuizzes = API.getQuizzes(current_category.equals("all") ?
                        "" : current_category, page);

                maxpage = (int) Math.ceil(jsonAPIQuizzes.getInt("count") / 20.0);
                JSONArray jsonArray = jsonAPIQuizzes.getJSONArray("tests"); //first page
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonQuiz = jsonArray.getJSONObject(i);
                    if (
                            (databaseHelper.addOrUpdateQuiz(
                                    new Quiz(
                                            jsonQuiz.getInt("test_id"),
                                            jsonQuiz.getString("test_name"),
                                            jsonQuiz.getString("image"),
                                            jsonQuiz.getString("description"),
                                            jsonQuiz.getString("category")
                                    )
                            ) > 0)
                                    && (page == 1)
                            ) changes = true; //если был insert (а не update) и стр. = 1
                }

                editor.putLong("quizzes_last_sync", new Date().getTime());
            } catch (Exception e) {
                System.out.println("in doin quizzes: " + e.toString());
                if (page >= maxpage)
                    maxpage = (int) Math.ceil(databaseHelper.getQuizzesCount() / 20.0);
                return false;
            } finally {
                if (changes) {
                    QuizAdapter = null;
                    initQuizzes(page);
                } else if (page > 1) initQuizzes(page); //если изменения в бд из стр.1 или
                // стр. != 1

                // TODO: 26.03.16 Сделать по свайпу вверх обновление данных, тогда можно
                // будет при клике на категорию брать данные из кеша и не проверять были
                // ли изменения
            }

            editor.apply();
            return true;
        }
    }

    public class QuizzesUnfinishedAsyncTask extends MainAsyncTask {

        @Override
        protected Boolean doInBackground(Integer... params) {
            try {
                String quizIds = API.getUnfinishedQuizIds();
                databaseHelper.setQuizzesStartedByStarted("1", "0");
                databaseHelper.setQuizzesStartedByIds("1", quizIds);
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                QuizAdapter = null;
                initQuizzes(1);
            }
        }

    }

}
