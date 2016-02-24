package com.seankrail.coursegrabber.android;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import com.seankrail.coursegrabber.R;
import com.seankrail.coursegrabber.java.CSVParserFileReader;
import com.seankrail.coursegrabber.java.ColorEnum;
import com.seankrail.coursegrabber.java.Event;
import com.seankrail.coursegrabber.java.Grabber;
import com.seankrail.coursegrabber.java.GrabberDebug;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Project: CourseGrabber
 * Author: Sean Krail
 * Author's Website: seankrail.com
 *
 * Modified on February 212, 2016 at 5:42 PM.
 * Created on 3/14/15
 */
public class UpdateTask extends AsyncTask<String, String, Boolean> {

    private UpdateActivity context;
    private String username;
    private String password;
    private ProgressDialog pd;
    private byte mode;
    private boolean forward;

    private static final String TAG = "UT";

    public UpdateTask(UpdateActivity context, String username, String password) {
        this.context = context;
        this.username = username;
        this.password = password;
        pd = new ProgressDialog(this.context);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setIndeterminate(false);
        pd.setCancelable(false);
        pd.setMax(100);
        pd.setTitle("Grabbing Courses");
        pd.setMessage("Initializing");
        pd.setProgress(0);

        publishProgress("Initializing the async task", "0");

        mode = 0;
        forward = true;

        Log.i(TAG + ".init", "ProgressTask created.");
    }

    @Override
    public void onPreExecute() {
        pd.show();
        Log.i(TAG + ".init", "ProgressTask pre executed.");
    }

    @Override
    public Boolean doInBackground(String... strings) {
        long start = System.currentTimeMillis();
        long t0 = start;
        publishProgress("Creating a file for your schedule", "0");

        Log.i(TAG + ".init", "started async task");
        File userFile = new File(this.context.getFilesDir(), "schedule.csv");
        try {
            if (!userFile.exists()) {
                publishProgress("File created", "5");
                userFile.createNewFile();
                Log.i(TAG + ".csv","created file");
            } else {
                publishProgress("File already exists", "5");
                Log.i(TAG + ".csv", "file already exists");
            }



            Log.i(TAG + ".time", "Time to open connection to download file: " + (System.currentTimeMillis() - t0));
            t0 = System.currentTimeMillis();


            publishProgress("Grabbing your courses...", "15");

            int res = GrabberDebug.grab(this.context, userFile, this.username, this.password, true);

            Log.i(TAG + ".grabber", "Grabber result: " + res);

            if (res != Grabber.SUCCESS) return Boolean.FALSE;


            Log.i(TAG + ".grabber", "Finished writing, CSV has been copied to the user's file.");

            publishProgress("Parsing the csv file for your schedule", "20");
            Log.i(TAG+".time","Time to download file: " + (System.currentTimeMillis() - t0));
            t0 = System.currentTimeMillis();

            CSVParserFileReader parser = new CSVParserFileReader(userFile);
            Log.i(TAG + ".csv", "Created CSVParser");
            Log.i(TAG + ".csv", "Proof of success (should read CISC275011): " + parser.getEvents().get(1).getSubject());
            int n = parser.getEvents().size();
            publishProgress("Deleting your old schedule's calendar", "30");
            Log.i(TAG + ".time", "Time to parse CSV file: " + (System.currentTimeMillis() - t0));
            t0 = System.currentTimeMillis();

            Log.i(TAG + ".csv", "Deleted CSV file");
            // File is no longer needed
            userFile.delete();





            // Delete current 'CourseGrabber' calendars, so to not make duplicate calendar
            Log.i(TAG + ".delete", "Deleting old 'CourseGrabber' calendar, if it exists.");

            String[] projection = new String[]{CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.NAME};
            Cursor calCursor = this.context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                    projection, CalendarContract.Calendars.VISIBLE + " = 1",
                    null, CalendarContract.Calendars._ID + " ASC");
            String selection = CalendarContract.Calendars.ACCOUNT_NAME + " = ? ";
            if (calCursor.moveToLast()) {

                int calCursorIndex = 0;

                do {
                    long id = calCursor.getLong(0);

                    String calCursorContent = "";
                    for (int i = 0; i < calCursor.getColumnNames().length ; i++) {
                        calCursorContent += "'" + calCursor.getColumnName(i) + "' = '" + calCursor.getString(i) + "';    ";
                    }
                    Log.i(TAG + ".delete", "Calendar #" + calCursorIndex + ": " + calCursorContent);

                    if (calCursor.getString(0).contentEquals("CourseGrabber")) { // ColumnIndex 0 is the Calendar's ACCOUNT_NAME
                        Log.i(TAG + ".delete", "Deleted 'CourseGrabber' calendar");
                        this.context.getContentResolver().delete(CalendarContract.Calendars.CONTENT_URI, selection, new String[]{"CourseGrabber"});
                        break;
                    } else if (calCursor.getString(0).contentEquals("Course Grabber")) {
                        Log.i(TAG + ".delete", "Deleted 'Course Grabber' calendar");
                        this.context.getContentResolver().delete(CalendarContract.Calendars.CONTENT_URI, selection, new String[]{"Course Grabber"});
                    }

                    calCursorIndex++;
                } while (calCursor.moveToPrevious());
            }
            calCursor.close();

            Log.i(TAG + ".time", "Time to delete old calendar: " + (System.currentTimeMillis() - t0));




            // Now, create 'CourseGrabber' calendar

            publishProgress("Creating a new calendar", "40");
            t0 = System.currentTimeMillis();

            // Now, create 'CourseGrabber' calendar
            Log.i(TAG + ".create", "Creating a new 'CourseGrabber' calendar.");

            Uri calUri = CalendarContract.Calendars.CONTENT_URI;
            ContentValues cv = new ContentValues();
            cv.put(CalendarContract.Calendars.ACCOUNT_NAME, "CourseGrabber");
            cv.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
            cv.put(CalendarContract.Calendars.NAME, "CourseGrabber");
            cv.put(CalendarContract.Calendars.OWNER_ACCOUNT, this.username + "@udel.edu");
            cv.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "My UD Schedule");
            ColorEnum color = ColorEnum.values()[(new Random()).nextInt(ColorEnum.values().length)];
            Log.i(TAG + ".color", "Calendar Color: " + color.getColor());
            cv.put(CalendarContract.Calendars.CALENDAR_COLOR, Color.parseColor(color.getColor()));
            cv.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
            cv.put(CalendarContract.Calendars.OWNER_ACCOUNT, true);
            cv.put(CalendarContract.Calendars.VISIBLE, 1);
            cv.put(CalendarContract.Calendars.SYNC_EVENTS, 1);

            calUri = calUri.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "CourseGrabber")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                    .build();

            Uri result = this.context.getContentResolver().insert(calUri, cv);

            cv.clear(); // recycle early

            Log.i(TAG + ".create", "Added a calendar with Uri result: " + result.toString());

            publishProgress("Adding your courses to the new calendar", "45");
            Log.i(TAG+".time","Time to create new calendar: " + (System.currentTimeMillis() - t0));
            t0 = System.currentTimeMillis();

            // Add id to new calendar

            projection = new String[] {CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars._ID};
            calCursor = this.context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                    projection, CalendarContract.Calendars.VISIBLE + " = 1",
                    null, CalendarContract.Calendars._ID + " ASC");
            long calendarID = -1;
            if (calCursor.moveToLast()) {
                do {
                    Log.i(TAG + ".create", "Calendar Cursor: " + calCursor.getString(0));
                    if (calCursor.getString(0).contentEquals("CourseGrabber")) {
                        calendarID = calCursor.getLong(1);
                        Log.i(TAG + ".create", "Added id(" + calendarID + ") to CourseGrabber calendars");
                        break;
                    }
                } while (calCursor.moveToPrevious());
            }
            calCursor.close();
            if (calendarID == -1) return Boolean.FALSE;

            Log.i(TAG+".time","Time to add id to new calendar: " + (System.currentTimeMillis() - t0));
            t0 = System.currentTimeMillis();



            // Add courses to 'CourseGrabber' calendar, THERE IS ONE AND ONLY ONE
            Log.i(TAG + ".courses", "Adding courses to the newly created 'CourseGrabber' calendar");

            double inc = ( ((double) (25)) / ((double) (n)) );
            Map<String, ColorEnum> uniqueSubjects = new TreeMap<String, ColorEnum>();
            Set<ColorEnum> uniqueColors = new TreeSet<ColorEnum>();
            for (ColorEnum ce : ColorEnum.values()) uniqueColors.add(ce);
            ArrayList<Event> events = parser.getEvents();
            Iterator<Event> iterator = events.iterator();
            Log.i(TAG + ".progress", "Size = " + events.size() + " and inc = " + inc);
            long eventID = -1;
            for (int i = 0; iterator.hasNext(); i++) {
                Event e = iterator.next();
                publishProgress("Adding " + e.getSubject() + " to the new calendar", (int) (inc * i + 51) + "");
                cv = new ContentValues();

                // Unique color for each subject
                if (uniqueSubjects.containsKey(e.getSubject())) {
                    cv.put(CalendarContract.Events.EVENT_COLOR, Color.parseColor(uniqueSubjects.get(e.getSubject()).getColor()));
                } else {
                    ColorEnum ce = (ColorEnum) uniqueColors.toArray()[(new Random()).nextInt(uniqueColors.size())];
                    cv.put(CalendarContract.Events.EVENT_COLOR, Color.parseColor(ce.getColor()));
                    uniqueSubjects.put(e.getSubject(), ce);
                    uniqueColors.remove(ce);
                    //Log.i(TAG + ".color", "Color: " + ce.getColor());
                }

                //Log.i(TAG + ".courses", "EventID: " + (eventID + i + 2) + ", Subject: " + e.getSubject() + ", Start: " + e.getStart() + ", End:" + e.getEnd() + ", AllDay: " + e.getAllDay() + ", Location: " + e.getLocation() + ", CalendarID: " + calendarID + ", TimeZone: " + TimeZone.getDefault().getID() + ", AccessLevel: " + CalendarContract.Events.ACCESS_CONFIDENTIAL + ".");
                //Log.i(TAG + ".courses", "" + (eventID + i + 2));
                //cv.put(CalendarContract.Events._ID, (eventID + i + 2));
                cv.put(CalendarContract.Events.TITLE, e.getSubject());
                cv.put(CalendarContract.Events.DTSTART, e.getStart());
                cv.put(CalendarContract.Events.DTEND, e.getEnd());
                cv.put(CalendarContract.Events.ALL_DAY, e.getAllDay());
                if (e.getLocation() != null) cv.put(CalendarContract.Events.EVENT_LOCATION, e.getLocation());
                cv.put(CalendarContract.Events.CALENDAR_ID, calendarID);
                cv.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
                cv.put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PUBLIC);

                //cv.put(CalendarContract.Events._ID, i);

                Uri uri = this.context.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, cv);
                cv.clear();

                //Log.i(TAG + ".courses", "Index " + i + ": Calendar_ID(" + calendarID + ")" + " Event_ID(" + (eventID + i + 2) + ")");
                Log.i(TAG + ".courses", "index(" + i + "), CalendarID(" + calendarID + "), Title(" + e.getSubject() + "), Start(" + e.getStart() + ")" );
            }

            publishProgress("Adding reminders to your new calendar", "75");
            Log.i(TAG+".time","Time to add 164 classes to the calendar: " + (System.currentTimeMillis() - t0));
            t0 = System.currentTimeMillis();

            boolean reminders = context.getSharedPreferences(context.getString(R.string.preference_file_key), context.MODE_PRIVATE).getBoolean("reminders", true);

            Log.i(TAG + ".reminders", "Add reminders? " + reminders);

            if (reminders) {
                inc = ( ((double) (25)) / ((double) (n)) );
                int i = 0;

                projection = new String[]{CalendarContract.Events.CALENDAR_ID, CalendarContract.Events._ID, CalendarContract.Events.ALL_DAY, CalendarContract.Events.TITLE, CalendarContract.Calendars.ACCOUNT_NAME};
                Cursor eventCursor = this.context.getContentResolver().query(CalendarContract.Events.CONTENT_URI,
                        projection, CalendarContract.Events.VISIBLE + " = 1",
                        null, CalendarContract.Events._ID + " ASC");

                // Get user's preference
                int reminder = context.getSharedPreferences(context.getString(R.string.preference_file_key), context.MODE_PRIVATE).getInt("reminder", 30);

                if (eventCursor.moveToFirst()) {
                    String displayName = "";
                    do {
                        Log.i(TAG + ".reminders", "Event info: CalendarID(" + eventCursor.getLong(0) + "), EventID(" + eventCursor.getLong(1) + "), AllDay(" + eventCursor.getInt(2) + "), Title('" + eventCursor.getString(3) + "'), AccountName('" + eventCursor.getString(4) +"')"
                                + ", Test1(" + (eventCursor.getString(4) == "CourseGrabber") + "), Test2(" + (eventCursor.getString(4).contentEquals("CourseGrabber")) + ")"
                                + ", Test3(" + (eventCursor.getLong(0) == calendarID) + "), Test4(" + (eventCursor.getInt(2) == 0) + "), Test5(" + (eventCursor.getInt(2) == 1) + ")");

                        if (!eventCursor.getString(4).contentEquals("CourseGrabber")) continue;
                        //Log.i(TAG + ".reminders", "Title: '" + eventCursor.getString(3) + "' All Day: '" + eventCursor.getString(2) + "'");
                        //Log.i(TAG + ".reminders", "Event info: CalendarID(" + eventCursor.getLong(0) + "), EventID(" + eventCursor.getLong(1) + "), AllDay(" + eventCursor.getInt(2) + "), Title(" + eventCursor.getString(3) +")");
                        if (eventCursor.getLong(0) == calendarID && eventCursor.getInt(2) == 0) {
                            publishProgress("Adding reminders to your new calendar", (int) (inc * i + 75) + "");
                            eventID = eventCursor.getLong(1);
                            Log.i(TAG + ".reminders", "Added event id(" + eventID + ")");
                            cv = new ContentValues();
                            cv.put(CalendarContract.Reminders.EVENT_ID, eventID);
                            cv.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                            cv.put(CalendarContract.Reminders.MINUTES, reminder);
                            Uri uri = this.context.getContentResolver().insert(CalendarContract.Reminders.CONTENT_URI, cv);
                            cv.clear();
                            //Log.i(TAG + ".reminders", "Reminder added.");
                            i++;
                        } else if (eventCursor.getLong(0) == calendarID && eventCursor.getInt(2) == 1) {
                            Log.i(TAG + ".reminders", "Skipped all day event.");
                            publishProgress("Skipping a reminder for an all-day event", (int) (inc * i + 75) + "");
                        } else {
                            publishProgress("ERROR: \"No such calendar.\"", (int) (inc * i + 75) + "");
                            Log.e(TAG + ".reminders", "Error.");
                            //Log.e(TAG + ".reminders", "No such calendar: '" + eventCursor.getLong(0) + ", " + eventCursor.getLong(1) + ", " + eventCursor.getInt(2) + ", " + eventCursor.getString(3) + "'"); // receiving this error
                        }
                    } while (eventCursor.moveToNext());
                }
                eventCursor.close();
            }
            publishProgress("Finished uploading your courses to the new calendar", "100");
            Log.i(TAG+".time","Time to add reminders: " + (System.currentTimeMillis() - t0));
            Log.i(TAG+".time", "Total Time: " + (System.currentTimeMillis() - start));
            return Boolean.TRUE;    // success

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Boolean.FALSE;   // failure
    }

    @Override
    protected void onProgressUpdate(String... strings) {
        switch (mode) {
            case ((byte) 0):
                pd.setMessage(strings[0] + '.');
                break;
            case ((byte) 1):
                pd.setMessage(strings[0] + "..");
                break;
            case ((byte) 2):
                pd.setMessage(strings[0] + "...");
                break;
            case ((byte) 3):
                pd.setMessage(strings[0] + "....");
                break;
            case ((byte) 4):
                pd.setMessage(strings[0] + ".....");
                break;
            case ((byte) 5):
                pd.setMessage(strings[0] + "......");
                break;
            case ((byte) 6):
                pd.setMessage(strings[0] + ".......");
                break;
            case ((byte) 7):
                pd.setMessage(strings[0] + "........");
        }
        if (forward && (++mode == (byte) 7)) forward = false;
        else if (!forward && (--mode == (byte) 0)) forward = true;
        pd.setProgress(Integer.parseInt(strings[1]));
    }

    @Override
    protected void onPostExecute(Boolean bool) {
        if (bool == Boolean.TRUE) {
            Toast.makeText(this.context, "Courses added successfully!", Toast.LENGTH_LONG).show();
            Log.i(TAG + ".progress", "ProgressTask post executed.");
        } else {
            Toast.makeText(this.context, "Error occurred, no such calendar exists", Toast.LENGTH_LONG).show();
            Log.i(TAG + ".progress", "ProgressTask failed to execute");
        }
        this.context.updateUI();
        pd.dismiss();
    }

    // End of UpdateTask's implemented functions

}
