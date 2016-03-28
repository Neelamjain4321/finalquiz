package ru.finalsoft.finalquiz;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private SharedPreferences loginPref;

    // UI references.
    private TextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private TextView mText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_login);
        loginPref = getSharedPreferences("user", MODE_PRIVATE);
        // Set up the login form.
        mPasswordView = (EditText) findViewById(R.id.password);
        mLoginFormView = findViewById(R.id.login_form);
        mText = (TextView) findViewById(R.id.text_view);
        mProgressView = findViewById(R.id.login_progress);

        showProgress(loginWithCookie()); //если есть куки, показывает прогрессбар

        mEmailView = (TextView) findViewById(R.id.email);
        if (!loginPref.getString("user_email", "").equals("")) {
            mEmailView.setText(loginPref.getString("user_email", ""));
            mPasswordView.requestFocus();
        }


        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        if (mEmailSignInButton != null)
            mEmailSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptLogin();
                }
            });

    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean loginWithCookie() {

        if (loginPref.getString("cookie", "").equals(""))
            return false;

        mAuthTask = new UserLoginTask(loginPref.getString("cookie", ""));
        mAuthTask.execute((Void) null);

        return true;
    }

    private boolean isEmailValid(String email) {
        return email.length() > 4;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    private void showText(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    mText.setVisibility(View.GONE);
                    mText.setText(text);
                    mText.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        });

    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        mText.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private String error;
        private JSONObject data = null;
        private eAPI API = eAPI.getInstance();

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        UserLoginTask(String cookie) {
            mEmail = mPassword = null;
            API.setCookie(cookie);
        }


        @Override
        protected Boolean doInBackground(Void... params) { //log in or register

            showText(getString(R.string.try_to_log_in));
            if (API.getCookie().equals("")) {
                try { //here we get new cookies and session
                    data = API.doLogin(mEmail, mPassword);
                    if (data.getBoolean("status")) {
                        data = API.getAccountInfo();
                        return true;
                    }

                    //we are going to register
                    if (mEmail.contains("@")) {
                        data = API.doRegister(mEmail, mPassword);
                        if (data.getBoolean("status")) {
                            showText(getString(R.string.registered_and_logged_in));
                            data = API.doLogin(mEmail, mPassword);
                            if (data.getBoolean("status")) {
                                data = API.getAccountInfo();
                                Thread.sleep(1000);
                                return true;
                            }
                        }
                        error = data.getString("error");
                    } else error = "error_invalid_email";

                } catch (IOException e) {
                    error = "_unable_to_resolve_host";
                } catch (JSONException e) {
                    error = "_server_response_error";
                } catch (Exception e) {
                    error = e.toString();
                }

                return false;
            } else { //here we get session

                try {
                    data = API.getAccountInfo();
                    return true;
                } catch (FileNotFoundException t) {
                    error = "_session_expired";
                    return false;
                } catch (Exception t) { //we can have No internet (UnknownHostException). Posibility to use this app without internet
                    System.out.println("Скорее всего нет соединения " + t.toString());
                }

            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
                //save cookies
                SharedPreferences.Editor ed = loginPref.edit();
                try {
                    ed.putString("cookie", API.getCookie());
                    ed.putInt("user_id", data.getInt("id"));
                    ed.putString("user_name", data.getString("user_name"));
                    ed.putString("user_email", data.getString("user_email"));
                    ed.putString("first_name", data.getString("first_name"));
                    ed.putString("last_name", data.getString("last_name"));
                    ed.putBoolean("feed", data.getString("feed").equals("1"));
                    ed.putInt("permission", data.getInt("permission"));
                } catch (JSONException e) {
                    System.out.println("JSON EXCEPTION ON POST EXECUTE: " + e);
                } catch (Exception e) {
                    System.out.println("On post execute " + e.toString());
                }
                ed.apply(); //save

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent); // показываем новое Activity //старое не сохраняется. т.к. в манифесте записано nostory = true
            } else {
                //снимаем progressbar; показываем ошибку;
                showProgress(false);
                try {
                    Class c = R.string.class;
                    if (error.charAt(0) == '_')
                        Snackbar.make(mLoginFormView, getString((int) c.getField(error).get(c)),
                                Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    else if (error.equals("error_invalid_email"))
                        mEmailView.setError(getString((int) c.getField(error).get(c)));
                    else mPasswordView.setError(getString((int) c.getField(error).get(c)));
                } catch (Exception e) {
                    Snackbar.make(mLoginFormView, getString(R.string.error) + ": " + error, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                SharedPreferences.Editor ed = loginPref.edit();

                if (mEmail == null || mEmail.equals(""))
                    mEmailView.setText(loginPref.getString("user_email", ""));
                else
                    mEmailView.setText(mEmail);

                if (error.equals("error_invalid_email"))
                    mEmailView.requestFocus();
                else mPasswordView.requestFocus();

                ed.clear();
                ed.putString("user_email", mEmail);
                ed.apply();
                API.setCookie("");


            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

