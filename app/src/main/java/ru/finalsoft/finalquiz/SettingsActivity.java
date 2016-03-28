package ru.finalsoft.finalquiz;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    SharedPreferences loginPref;
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            String key = preference.getKey();

            if ("logout".equals(key))
                return true;
            else if ("feed".equals(key)) {
                loginPref.edit().putBoolean(key, stringValue.equals("true")).apply();
                return true;
            } else loginPref.edit().putString(key, stringValue).apply();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        if ("feed".equals(preference.getKey())) return;

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        setTitle(getString(R.string.title_activity_settings));
        addPreferencesFromResource(R.xml.pref_general);

        loginPref = getSharedPreferences("user", MODE_PRIVATE);

        EditTextPreference version = (EditTextPreference) findPreference("version");
        try {
            version.setText(getString(R.string.current_version, getPackageManager().getPackageInfo
                    (getPackageName(), 0).versionName));
        } catch (Exception e) {
            version.setText(getString(R.string.current_version, "1.0"));
        }

        SwitchPreference notifications = (SwitchPreference) findPreference("feed");
        notifications.setChecked(loginPref.getBoolean("feed", false));

        ListPreference sync_freq = (ListPreference) findPreference("sync_frequency");
        sync_freq.setValue(loginPref.getString("sync_frequency", "1440"));

        Preference logoutBtn = findPreference("logout");
        logoutBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DBHelper.getInstance(getApplicationContext()).removeSavedUserResults(loginPref.getInt("user_id", 0));
                DBHelper.getInstance(getApplicationContext()).removeQuizzes();
                String email = loginPref.getString("user_email", "");
                loginPref.edit().clear().putString("user_email", email).apply();
                eAPI.getInstance().setCookie(null);
                getSharedPreferences("app", MODE_PRIVATE).edit().clear().apply();
                Intent i = new Intent(getBaseContext(), LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            }
        });


        bindPreferenceSummaryToValue(version);
        bindPreferenceSummaryToValue(notifications);
        bindPreferenceSummaryToValue(sync_freq);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    /*
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }
    */

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || SettingsActivity.class.getName().equals(fragmentName);
    }

}
