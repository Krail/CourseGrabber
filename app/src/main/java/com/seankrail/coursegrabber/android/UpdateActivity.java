package com.seankrail.coursegrabber.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.seankrail.coursegrabber.R;


/**
 * Project: CourseGrabber
 * Package: ${PACKAGE_NAME}
 *
 * Created by Sean Krail on June 21, 2015 at 6:38 PM.
 */
public class UpdateActivity extends Activity {
    // Application Preferences
    private SharedPreferences preferences;
    // Main Activity
    private TextView title;
    private TextView subject;
    private Button btnSchedule;
    private Button btnSettings;
    private ProgressBar spinner;
    // Settings/Reminders Dialog
    private Dialog dialog;
    private Switch aSwitch;
    private TextView seekBarTextView;
    private SeekBar seekBar;

    private static final String TAG = "UpdateActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_updateschedule);

        SharedPreferences spePreferences = this.getSharedPreferences("CookiePrefsFile", MODE_PRIVATE);
        spePreferences.edit().clear().commit();
        //spePreferences.edit().clear();

        init();

        Log.i(TAG, "activity created");
    }

    private void init() {
        initActivity();
        initDialog();
    }

    private void initActivity() {
        title = (TextView) findViewById(R.id.coursegrabbertitle);
        subject = (TextView) findViewById(R.id.updateScheduleStringTextView);
        EditText usernameEditText = (EditText) findViewById(R.id.updateScheduleUsernameEditText);
        EditText passwordEditText = (EditText) findViewById(R.id.updateSchedulePasswordEditText);
        btnSchedule = (Button) findViewById(R.id.updateScheduleButton);
        btnSettings = (Button) findViewById(R.id.updateSettingsButton);
        spinner = (ProgressBar) findViewById(R.id.progressBar);

        View.OnFocusChangeListener editTextFocusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !((EditText) v).getText().toString().isEmpty()) {
                    ((EditText) v).setCursorVisible(true);
                    ((EditText) v).setSelection(((EditText) v).length());
                }
                else ((EditText) v).setCursorVisible(false);
            }
        };
        View.OnKeyListener editTextKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (((EditText) v).getText().toString().isEmpty()) ((EditText) v).setCursorVisible(false);
                else ((EditText) v).setCursorVisible(true);
                return false;
            }
        };

        usernameEditText.setOnFocusChangeListener(editTextFocusListener);
        passwordEditText.setOnFocusChangeListener(editTextFocusListener);

        usernameEditText.setOnKeyListener(editTextKeyListener);
        passwordEditText.setOnKeyListener(editTextKeyListener);

        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    Log.i(TAG, "Settings btnDialog clicked through password(EditText) action");
                    btnSettings.performClick();
                    return true;
                }
                return false;
            }
        });

        btnSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Update schedule btnSchedule clicked");
                (new UpdateTask((UpdateActivity) (v.getContext()))).execute();

                // Update Calendar with UpdateTask.java
                showSpinner();


                //task = new UpdateTask((UpdateActivity) v.getContext());
                //task.execute();
            }
        });
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Settings btnDialog clicked");
                //context.startSettingsActivity((UpdateActivity) ((Button) v).getContext());
                /*
                Intent intent = new Intent(v.getContext(), SettingsActivity.class);
                startActivity(intent);
                */
                dialog.show();
            }
        });
    }

    private void initDialog() {
        //dialog = new Dialog(this, R.style.Theme_AppCompat_Dialog);
        //dialog = new Dialog(this, R.style.Theme_AppCompat_Light_Dialog);
        dialog = new Dialog(this);
        dialog.setTitle("Settings");
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.layout_settings);

        preferences = getSharedPreferences(getString(R.string.preference_file_key), com.seankrail.coursegrabber.android.UpdateActivity.MODE_PRIVATE);

        if (!preferences.contains("reminder")) preferences.edit().putInt("reminder", 15).apply();
        int reminder = preferences.getInt("reminder", 15);

        if (!preferences.contains("reminders")) preferences.edit().putBoolean("reminders", true).apply();

        TextView switchText = (TextView) dialog.findViewById(R.id.switchText);
        aSwitch = (Switch) dialog.findViewById(R.id.switch1);
        seekBarTextView = (TextView) dialog.findViewById(R.id.seekBarTextView);
        seekBar = (SeekBar) dialog.findViewById(R.id.seekBar);
        Button btnDialog = (Button) dialog.findViewById(R.id.button1);


        switchText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aSwitch.toggle();
            }
        });

        aSwitch.setChecked(preferences.getBoolean("reminders", true));
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean("reminders", isChecked).apply();
                seekBar.setEnabled(isChecked);
                if (isChecked) {
                    seekBarTextView.setText("Remind me " + (seekBar.getProgress() + 5) + " minutes before class starts");
                    seekBar.setVisibility(View.VISIBLE);
                } else {
                    seekBarTextView.setText("Do not remind me before class starts");
                    //seekBarTextView.setText("Reminders are turned off");
                    seekBar.setVisibility(View.INVISIBLE);
                }
            }
        });

        seekBar.setMax(55);
        seekBar.setProgress(reminder - 5);
        if (aSwitch.isChecked()) {
            seekBarTextView.setText("Remind me " + (seekBar.getProgress() + 5) + " minutes before class starts");
            seekBar.setEnabled(true);
            seekBar.setVisibility(View.VISIBLE);
        }
        else {
            seekBarTextView.setText("Do not remind me before class starts");
            //seekBarTextView.setText("Reminders are turned off");
            seekBar.setEnabled(false);
            seekBar.setVisibility(View.INVISIBLE);
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress % 5 == 0) seekBar.setProgress(progress);
                else seekBar.setProgress(progress - (progress % 5));
                seekBarTextView.setText("Remind me " + (seekBar.getProgress() + 5) + " minutes before class starts");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                preferences.edit().putInt("reminder", (seekBar.getProgress() + 5)).apply();
            }
        });

        btnDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "btnDialog clicked");

                preferences.edit().putBoolean("reminders", aSwitch.isChecked()).apply();
                preferences.edit().putInt("reminder", (seekBar.getProgress() + 5)).apply();

                dialog.dismiss();
            }
        });
    }

    private void showSpinner() { // Show spinner
        this.btnSchedule.setVisibility(View.GONE);
        this.btnSettings.setVisibility(View.GONE);
        this.spinner.setVisibility(View.VISIBLE);
    }

    private void showButtons() {
        this.spinner.setVisibility(View.GONE);
        this.btnSchedule.setVisibility(View.VISIBLE);
        this.btnSettings.setVisibility(View.VISIBLE);
    }

    public void updateUI() {
        title.setText("CoursesGrabbed");
        title.setGravity(Gravity.CENTER);
        subject.setText("Your schedule is up-to-date");
        subject.setGravity(Gravity.CENTER);
        this.btnSchedule.setText("Schedule Updated");
        showButtons();
    }
}
