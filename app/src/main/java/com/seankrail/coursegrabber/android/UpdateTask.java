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
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Project: Course Grabber
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
        publishProgress("Creating a file for your schedule", "0");

        Log.i(TAG + ".init", "started async task");
        long t0 = System.currentTimeMillis();
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
            Log.i(TAG + ".time", "Time to create file for CSV: " + (System.currentTimeMillis() - t0) + " milliseconds.");


            publishProgress("Grabbing your courses...", "10");

            t0 = System.currentTimeMillis();
            int res = GrabberDebug.grab(this.context, userFile, this.username, this.password, true);
            Log.i(TAG+".time","Time to grab courses from UD servers: " + (System.currentTimeMillis() - t0) + " milliseconds.");

            Log.i(TAG + ".grab()", "Grabber result: " + res);

            if (res != Grabber.SUCCESS) return Boolean.FALSE;


            Log.i(TAG + ".grab()", "Finished writing, CSV has been copied to the user's file.");

            publishProgress("Parsing the csv file for your schedule", "20");

            t0 = System.currentTimeMillis();
            CSVParserFileReader parser = new CSVParserFileReader(userFile);
            Log.i(TAG + ".time", "Time to parse CSV file: " + (System.currentTimeMillis() - t0) + " milliseconds.");
            Log.i(TAG + ".csv", "Created CSVParser");

            Log.i(TAG + ".csv", "Deleted CSV file");
            // File is no longer needed
            t0 = System.currentTimeMillis();
            userFile.delete();
            Log.i(TAG + ".time", "Time to delete CSV file: " + (System.currentTimeMillis() - t0) + " milliseconds.");


            publishProgress("Checking if this calendar already exists", "25");
            t0 = System.currentTimeMillis();
            long calendarID = this.getCalendar();
            Log.i(TAG + ".time", "Time to get calendar id / check if there is a calendar: " + (System.currentTimeMillis() - t0) + " milliseconds.");

            t0 = System.currentTimeMillis();
            if (calendarID != -1) {
                publishProgress("Calendar exists, deleting its events", "30");
                this.deleteEvents(calendarID);
                Log.i(TAG + ".time", "Time to delete events: " + (System.currentTimeMillis() - t0) + " milliseconds.");
            } else {
                publishProgress("Calendar doesn't exist, creating it", "30");
                calendarID = this.createCalendar();
                Log.i(TAG + ".time", "Time to create calendar: " + (System.currentTimeMillis() - t0) + " milliseconds.");
            }


            publishProgress("Adding events to calendar", "35");
            t0 = System.currentTimeMillis();
            this.addEvents(calendarID, parser.getEvents(), 35, 100);
            Log.i(TAG + ".time", "Time to add events: " + (System.currentTimeMillis() - t0) + " milliseconds.");
            t0 = System.currentTimeMillis();

            Log.i(TAG+".time", "Total Time: " + (t0 - start) + " milliseconds.");

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

    private long getCalendar() {

        Log.i(TAG + ".getCalendar()", "Called getCalendar().");

        Cursor calendarCursor = this.context.getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,                                                 // Uri uri
                new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.ACCOUNT_NAME},  // String[] projection
                CalendarContract.Calendars.ACCOUNT_NAME + "=?",                                         // String selection
                new String[]{"Course Grabber"},                                                         // String[] selectionArgs
                CalendarContract.Calendars._ID + " ASC");                                               // String sortOrder

        if (calendarCursor.moveToLast()) {

            int calCursorIndex = 0; // debug variable

            do {

                // DEBUG start
                String calCursorContent = "";
                for (int i = 0; i < calendarCursor.getColumnNames().length ; i++) {
                    calCursorContent += "'" + calendarCursor.getColumnName(i) + "' = '" + calendarCursor.getString(i) + "';    ";
                }
                Log.i(TAG + ".getCalendar()", "Calendar #" + calCursorIndex++ + ": " + calCursorContent);
                // DEBUG end

                if (calendarCursor.getString(1).contentEquals("Course Grabber")) {
                    //this.context.getContentResolver().delete(
                    //        CalendarContract.Calendars.CONTENT_URI,         // Uri uri
                    //        CalendarContract.Calendars.ACCOUNT_NAME + "=?", // String where
                    //        new String[]{"Course Grabber"});                // String[] selectionArgs
                    Log.i(TAG + ".getCalendar()", "Found 'Course Grabber' calendar, must delete all of its events.");
                    return calendarCursor.getLong(0); // success
                }

            } while (calendarCursor.moveToPrevious());
        }
        calendarCursor.close();

        Log.i(TAG + ".getCalendar()", "Did not find 'Course Grabber' calendar , must create one.");

        return -1; // failure
    }

    private void deleteEvents(long calendarID) {

        Log.i(TAG + ".deleteEvents()", "Called deleteEvents(" + calendarID + ").");

        int numDeleted = this.context.getContentResolver().delete(
                CalendarContract.Events.CONTENT_URI,        // Uri uri
                CalendarContract.Events.CALENDAR_ID + "=? AND " + CalendarContract.Events.DELETED + "=?",         // String where
                new String[]{String.valueOf(calendarID), "0"});    // String[] selectionArgs

        Log.i(TAG + ".deleteEvents()", "Deleted all of the calendar's " + numDeleted + " events.");
    }

    private long createCalendar() {

        Log.i(TAG + ".createCalendar()", "Called createCalendar().");

        ColorEnum color = ColorEnum.values()[(new Random()).nextInt(ColorEnum.values().length)];
        Log.i(TAG + ".createCalendar()", "Generated the random color " + color.getColor() + " for the calendar.");

        ContentValues cv = new ContentValues(11);
        cv.put(CalendarContract.Calendars.ACCOUNT_NAME, "Course Grabber");
        cv.put(CalendarContract.Calendars.NAME, "Course Grabber");
        cv.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        cv.put(CalendarContract.Calendars.OWNER_ACCOUNT, this.username + "@udel.edu");
        cv.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "My UD Schedule");
        cv.put(CalendarContract.Calendars.CALENDAR_COLOR, Color.parseColor(color.getColor()));
        cv.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        cv.put(CalendarContract.Calendars.OWNER_ACCOUNT, true);
        cv.put(CalendarContract.Calendars.VISIBLE, 1);
        cv.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        cv.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().getID());

        Uri uri = this.context.getContentResolver().insert(
                CalendarContract.Calendars.CONTENT_URI.buildUpon()
                        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "Course Grabber")
                        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                        .build(),
                cv);

        cv.clear(); // recycle

        Log.i(TAG + ".createCalendar()", "Created calendar with id of " + uri.getLastPathSegment() + ".");

        return Long.parseLong(uri.getLastPathSegment());
    }

    private void addEvents(long calendarID, ArrayList<Event> events, int startProgress, int endProgress) {

        Log.i(TAG + ".addEvents()", "Called addEvents(" + calendarID + ", " + events.size() + " events).");

        double delta = ( ((double) (endProgress - startProgress)) / ((double) (events.size())) ); // increase in progress per event

        boolean reminders = context.getSharedPreferences(context.getString(R.string.preference_file_key), context.MODE_PRIVATE).getBoolean("reminders", true);
        int reminder = reminders
                ? context.getSharedPreferences(context.getString(R.string.preference_file_key), context.MODE_PRIVATE).getInt("reminder", 30)
                : -1;

        if (reminders) Log.i(TAG + ".addEvents()", "There are reminders with a notice of " + reminder + " minutes.");
        else Log.i(TAG + ".addEvents()", "There are no reminders.");

        // Build array of unique colors
        Map<String, ColorEnum> uniqueSubjects = new TreeMap<String, ColorEnum>();
        Set<ColorEnum> uniqueColors = new TreeSet<ColorEnum>();
        for (ColorEnum ce : ColorEnum.values()) uniqueColors.add(ce);

        ContentValues cv = new ContentValues(10);
        Iterator<Event> iterator = events.iterator();
        for (int i = 0; iterator.hasNext(); i++) {
            Event e = iterator.next();

            publishProgress("Adding " + e.getSubject() + "'s events to the calendar", String.valueOf( (int) (delta * i) + startProgress ));

            // Unique color for each subject
            if (uniqueSubjects.containsKey(e.getSubject())) { // Not a new subject, get its color
                cv.put(CalendarContract.Events.EVENT_COLOR, Color.parseColor(uniqueSubjects.get(e.getSubject()).getColor()));
            } else { // Repeated subject, get a new unique color
                ColorEnum ce = (ColorEnum) uniqueColors.toArray()[(new Random()).nextInt(uniqueColors.size())];
                cv.put(CalendarContract.Events.EVENT_COLOR, Color.parseColor(ce.getColor()));
                uniqueSubjects.put(e.getSubject(), ce);
                uniqueColors.remove(ce);
            }

            cv.put(CalendarContract.Events.TITLE, e.getSubject());
            cv.put(CalendarContract.Events.DTSTART, e.getStart());
            cv.put(CalendarContract.Events.DTEND, e.getEnd());
            cv.put(CalendarContract.Events.ALL_DAY, e.getAllDay());
            if (e.getLocation() != null) cv.put(CalendarContract.Events.EVENT_LOCATION, e.getLocation());
            cv.put(CalendarContract.Events.CALENDAR_ID, calendarID);
            cv.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
            cv.put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PUBLIC);
            if (e.getAllDay() == 1) cv.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE); // All day (holiday)
            else cv.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY); // Not all day (class/lab)

            Uri uri = this.context.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, cv);
            cv.clear(); // recycle early

            if (reminders) {
                long eventID = Long.parseLong(uri.getLastPathSegment());
                cv.put(CalendarContract.Reminders.EVENT_ID, eventID);
                cv.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                cv.put(CalendarContract.Reminders.MINUTES, reminder);
                this.context.getContentResolver().insert(CalendarContract.Reminders.CONTENT_URI, cv); // don't need returned URI
                cv.clear(); // recycle
            }
        }

        if (reminders) Log.i(TAG + ".addEvents()", "Added events without reminders.");
        else Log.i(TAG + ".addEvents()", "Added events with reminders.");

    }

}
