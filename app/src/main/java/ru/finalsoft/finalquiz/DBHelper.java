package ru.finalsoft.finalquiz;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {
    // Database Info
    private static final String DATABASE_NAME = "quizzesDatabase";
    private static final int DATABASE_VERSION = 17;

    // Table Names
    private static final String TABLE_QUIZZES = "quizzes";
    private static final String TABLE_USER_RESULTS = "user_results";

    // Quizzes Table Columns
    private static final String KEY_QUIZ_ID = "test_id";
    private static final String KEY_QUIZ_NAME = "test_name";
    private static final String KEY_QUIZ_IMAGE = "image";
    private static final String KEY_QUIZ_DESCRIPTION = "description";
    private static final String KEY_QUIZ_CATEGORY = "category";
    private static final String KEY_QUIZ_STARTED = "started";

    // User results Table Columns
    private static final String KEY_USER_RESULT_ID = "id";
    private static final String KEY_USER_RESULT_ONLINE_ID = "result_online_id";
    private static final String KEY_USER_RESULT_USER_ID = "user_id";
    private static final String KEY_USER_RESULT_QUIZ_ID = "test_id";
    private static final String KEY_USER_RESULT_QUIZ_NAME = "test_name";
    private static final String KEY_USER_RESULT_MIN_SCORE = "min";
    private static final String KEY_USER_RESULT_CURRENT_SCORE = "current";
    private static final String KEY_USER_RESULT_MAX_SCORE = "max";
    private static final String KEY_USER_RESULT_DATE = "date";
    private static final String KEY_USER_RESULT_DESCRIPTION = "description";
    private static final String KEY_USER_RESULT_PENDING = "pending";
    private static final String KEY_USER_RESULT_ANSWERS = "answers";

    private static final int QUIZZES_PER_PAGE = 20;
    private static final int USER_RESULTS_PER_PAGE = 50;

    private static DBHelper sInstance;

    /**
     * Constructor should be private to prevent direct instantiation.
     * Make a call to the static method "getInstance()" instead.
     */
    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DBHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DBHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    // Called when the database connection is being configured.
    // Configure database settings for things like foreign key support, write-ahead logging, etc.
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // Called when the database is created for the FIRST time.
    // If a database already exists on disk with the same DATABASE_NAME, this method will NOT be called.
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_QUIZZES_TABLE = "CREATE TABLE " + TABLE_QUIZZES +
                "(" +
                KEY_QUIZ_ID + " INTEGER PRIMARY KEY," + // Define a primary key
                KEY_QUIZ_NAME + " TEXT," +
                KEY_QUIZ_IMAGE + " TEXT," +
                KEY_QUIZ_DESCRIPTION + " TEXT," +
                KEY_QUIZ_CATEGORY + " TEXT," +
                KEY_QUIZ_STARTED + " INTEGER DEFAULT 0" +
                ")";
        String CREATE_USER_RESULTS_TABLE = "CREATE TABLE " + TABLE_USER_RESULTS +
                "(" +
                KEY_USER_RESULT_ID + " INTEGER PRIMARY KEY," + // Define a primary key
                KEY_USER_RESULT_USER_ID + " INTEGER," +
                KEY_USER_RESULT_ONLINE_ID + " INTEGER," +
                KEY_USER_RESULT_QUIZ_ID + " INTEGER," +
                KEY_USER_RESULT_QUIZ_NAME + " TEXT," +
                KEY_USER_RESULT_MIN_SCORE + " INTEGER," +
                KEY_USER_RESULT_CURRENT_SCORE + " INTEGER," +
                KEY_USER_RESULT_MAX_SCORE + " INTEGER," +
                KEY_USER_RESULT_DATE + " TEXT," +
                KEY_USER_RESULT_DESCRIPTION + " TEXT," +
                KEY_USER_RESULT_PENDING + " TEXT," +
                KEY_USER_RESULT_ANSWERS + " TEXT" +
                ")";

        db.execSQL(CREATE_QUIZZES_TABLE);
        db.execSQL(CREATE_USER_RESULTS_TABLE);

    }

    // Called when the database needs to be upgraded.
    // This method will only be called if a database already exists on disk with the same DATABASE_NAME,
    // but the DATABASE_VERSION is different than the version of the database that exists on disk.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            // Simplest implementation is to drop all old tables and recreate them
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUIZZES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_RESULTS);
            onCreate(db);
        }
    }

    // Insert or update a quiz
    public long addOrUpdateQuiz(Quiz quiz) {
        // Create and/or open the database for writing
        SQLiteDatabase db = getWritableDatabase();
        long added = 0;

        // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
        // consistency of the database.
        db.beginTransaction();
        try {
            // The user might already exist in the database (i.e. the same user created multiple posts).

            ContentValues values = new ContentValues();
            values.put(KEY_QUIZ_ID, quiz.test_id);
            values.put(KEY_QUIZ_NAME, quiz.test_name);
            values.put(KEY_QUIZ_IMAGE, quiz.image);
            values.put(KEY_QUIZ_DESCRIPTION, quiz.description);
            values.put(KEY_QUIZ_CATEGORY, quiz.category);

            int rows = db.update(TABLE_QUIZZES, values, KEY_QUIZ_ID + "= ?", new String[]{Integer
                    .toString(quiz.test_id)});

            if (rows == 1) {
                db.setTransactionSuccessful();
            } else {
                // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
                added = db.insertOrThrow(TABLE_QUIZZES, null, values);
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            System.out.println("Error while trying to add quiz to database: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return added;
    }

    public void setQuizzesStartedByIds(String started, String quiz_ids) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            db.execSQL("UPDATE " + TABLE_QUIZZES + " SET " + KEY_QUIZ_STARTED + " = " + started
                    + (quiz_ids == null ? "" : " WHERE " + KEY_QUIZ_ID + " IN(" + quiz_ids + ")"));
        } catch (Exception e) {
            System.out.println("Error while trying to update quiz.started in db: " + e.toString());
        }
    }

    public void setQuizzesStartedByStarted(String startedFrom, String startedTo) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            db.execSQL("UPDATE " + TABLE_QUIZZES + " SET " + KEY_QUIZ_STARTED + " = " + startedTo
                    + " WHERE " + KEY_QUIZ_STARTED + " = ?", new String[]{startedFrom});
        } catch (Exception e) {
            System.out.println("Error while trying to update quiz.started in db: " + e.toString());
        }
    }


    // Get quizzes in the database
    public List<Quiz> getQuizList(String category, int page) {
        List<Quiz> quizzes = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor;

        String QUIZZES_SELECT_QUERY = String.format("SELECT * FROM %s", TABLE_QUIZZES);

        if (category.equals("all")) {
            if (page > 0)
                QUIZZES_SELECT_QUERY += " LIMIT " + QUIZZES_PER_PAGE + " OFFSET "
                        + Integer.toString((page - 1) * QUIZZES_PER_PAGE);
            cursor = db.rawQuery(QUIZZES_SELECT_QUERY, null);
        } else { //if we show quizzes from a category.
            QUIZZES_SELECT_QUERY += String.format(" WHERE %s = ?", KEY_QUIZ_CATEGORY);
            if (page > 0)
                QUIZZES_SELECT_QUERY += " LIMIT " + QUIZZES_PER_PAGE + " OFFSET "
                        + Integer.toString((page - 1) * QUIZZES_PER_PAGE);
            cursor = db.rawQuery(QUIZZES_SELECT_QUERY, new String[]{category});
        }

        // "getReadableDatabase()" and "getWriteableDatabase()" return the same object (except under low
        // disk space scenarios)

        try {
            if (cursor.moveToFirst()) {
                do {
                    Quiz newQuiz = new Quiz(cursor.getInt(cursor.getColumnIndex(KEY_QUIZ_ID)),
                            cursor.getString(cursor.getColumnIndex(KEY_QUIZ_NAME)),
                            cursor.getString(cursor.getColumnIndex(KEY_QUIZ_IMAGE)),
                            cursor.getString(cursor.getColumnIndex(KEY_QUIZ_DESCRIPTION)),
                            cursor.getString(cursor.getColumnIndex(KEY_QUIZ_CATEGORY)));

                    newQuiz.started = cursor.getInt(cursor.getColumnIndex(KEY_QUIZ_STARTED));

                    quizzes.add(newQuiz);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d("TAG", "Error while trying to get posts from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return quizzes;
    }


    // Insert or update an user result
    public long addOrUpdateUserResult(UserResult userResult) {
        // Create and/or open the database for writing
        SQLiteDatabase db = getWritableDatabase();
        long added = userResult.result_id;

        // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
        // consistency of the database.
        db.beginTransaction();
        try {
            // The user might already exist in the database (i.e. the same user created multiple posts).

            ContentValues values = new ContentValues();
            values.put(KEY_USER_RESULT_ONLINE_ID, userResult.result_online_id);
            values.put(KEY_USER_RESULT_USER_ID, userResult.user_id);
            values.put(KEY_USER_RESULT_QUIZ_ID, userResult.test_id);
            values.put(KEY_USER_RESULT_QUIZ_NAME, userResult.test_name);
            values.put(KEY_USER_RESULT_MIN_SCORE, userResult.min);
            values.put(KEY_USER_RESULT_CURRENT_SCORE, userResult.current);
            values.put(KEY_USER_RESULT_MAX_SCORE, userResult.max);

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            values.put(KEY_USER_RESULT_DATE, df.format(userResult.date));

            values.put(KEY_USER_RESULT_DESCRIPTION, userResult.description);
            values.put(KEY_USER_RESULT_PENDING, userResult.pending);
            values.put(KEY_USER_RESULT_ANSWERS, userResult.answers);

            int rows = (userResult.result_online_id == 0) ? 0 : (
                    (userResult.result_id > 0) ?
                            db.update(TABLE_USER_RESULTS, values, KEY_USER_RESULT_ID + "= ?",
                                    new String[]{Integer.toString(userResult.result_id)}) :
                            db.update(TABLE_USER_RESULTS, values, KEY_USER_RESULT_ONLINE_ID + "= ?",
                                    new String[]{Integer.toString(userResult.result_online_id)})
            );

            if (rows == 1) {
                db.setTransactionSuccessful();
            } else {
                // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
                added = db.insertOrThrow(TABLE_USER_RESULTS, null, values);
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            System.out.println("Error while trying to add post to database: " + e.toString());
        } finally {
            db.endTransaction();
        }
        return added;
    }

    public List<UserResult> getUnsavedUserResultList(int user_id, int page) {
        return getUserResultsByQuery(KEY_USER_RESULT_ONLINE_ID + " = 0" +
                (user_id > 0 ? " AND " + KEY_USER_RESULT_USER_ID + " = " + user_id : ""), page);
    }

    public List<UserResult> getUnsavedUserResultListByQuiz(int user_id, int test_id, int page) {
        return getUserResultsByQuery(KEY_USER_RESULT_ONLINE_ID + " = 0" +
                (user_id > 0 ? " AND " + KEY_USER_RESULT_USER_ID + " = " + user_id : "") +
                (test_id > 0 ? " AND " + KEY_USER_RESULT_QUIZ_ID + " = " + test_id : ""), page);
    }

    public List<UserResult> getUserResultList(int user_id, int page) {
        return getUserResultsByQuery(user_id > 0 ? KEY_USER_RESULT_USER_ID + " = " + user_id : null, page);
    }

    public long getUserResultsCount(int user_id) {
        SQLiteDatabase db = getReadableDatabase();

        SQLiteStatement statement = db.compileStatement("SELECT COUNT(*) FROM " + TABLE_USER_RESULTS
                + (user_id > 0 ? " WHERE " + KEY_USER_RESULT_USER_ID + " = " + user_id : ""));

        return statement.simpleQueryForLong();
    }

    public long getQuizzesCount() {
        SQLiteDatabase db = getReadableDatabase();
        SQLiteStatement statement = db.compileStatement("SELECT COUNT(*) FROM " + TABLE_QUIZZES);
        return statement.simpleQueryForLong();
    }

    public int removeSavedUserResults(int user_id) {
        SQLiteDatabase db = getReadableDatabase();
        return db.delete(TABLE_USER_RESULTS, "user_id = " + user_id + " AND " +
                KEY_USER_RESULT_ONLINE_ID + " > 0", null);
    }

    public int removeUserResultById(long result_id) {
        SQLiteDatabase db = getReadableDatabase();
        return db.delete(TABLE_USER_RESULTS, KEY_USER_RESULT_ID + " = " + result_id, null);
    }

    public int removeQuizzes() {
        SQLiteDatabase db = getReadableDatabase();
        return db.delete(TABLE_QUIZZES, null, null);
    }

    // Get user results in the database
    private List<UserResult> getUserResultsByQuery(String whereClause, int page) {
        List<UserResult> userResults = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor;

        String USER_RESULTS_SELECT_QUERY = String.format("SELECT * FROM %s", TABLE_USER_RESULTS);

        if (whereClause != null)
            USER_RESULTS_SELECT_QUERY += " WHERE " + whereClause;

        USER_RESULTS_SELECT_QUERY += " ORDER BY " + KEY_USER_RESULT_ONLINE_ID + " = 0 DESC, " +
                KEY_USER_RESULT_ONLINE_ID + " DESC, " + KEY_USER_RESULT_ID + " DESC";

        if (page > 0)
            USER_RESULTS_SELECT_QUERY += " LIMIT " + USER_RESULTS_PER_PAGE + " OFFSET "
                    + Integer.toString((page - 1) * USER_RESULTS_PER_PAGE);

        cursor = db.rawQuery(USER_RESULTS_SELECT_QUERY, null);

        // "getReadableDatabase()" and "getWriteableDatabase()" return the same object (except under low
        // disk space scenarios)

        try {
            if (cursor.moveToFirst()) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                do {
                    UserResult newUserResult = new UserResult(
                            cursor.getInt(cursor.getColumnIndex(KEY_USER_RESULT_ID)),
                            cursor.getInt(cursor.getColumnIndex(KEY_USER_RESULT_ONLINE_ID)),
                            cursor.getInt(cursor.getColumnIndex(KEY_USER_RESULT_USER_ID)),
                            cursor.getInt(cursor.getColumnIndex(KEY_USER_RESULT_QUIZ_ID)),
                            cursor.getString(cursor.getColumnIndex(KEY_USER_RESULT_QUIZ_NAME)),
                            cursor.getInt(cursor.getColumnIndex(KEY_USER_RESULT_MIN_SCORE)),
                            cursor.getInt(cursor.getColumnIndex(KEY_USER_RESULT_CURRENT_SCORE)),
                            cursor.getInt(cursor.getColumnIndex(KEY_USER_RESULT_MAX_SCORE)),
                            df.parse(cursor.getString(cursor.getColumnIndex(KEY_USER_RESULT_DATE))),
                            cursor.getString(cursor.getColumnIndex(KEY_USER_RESULT_DESCRIPTION)),
                            cursor.getInt(cursor.getColumnIndex(KEY_USER_RESULT_PENDING)),
                            cursor.getString(cursor.getColumnIndex(KEY_USER_RESULT_ANSWERS)));
                    userResults.add(newUserResult);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            System.out.println("Error while trying to get posts from database: " + e.toString());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return userResults;
    }
}