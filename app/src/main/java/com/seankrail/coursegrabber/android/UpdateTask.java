package com.seankrail.coursegrabber.android;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.seankrail.coursegrabber.R;
import com.seankrail.coursegrabber.java.CSVParserFileReader;
import com.seankrail.coursegrabber.java.ColorEnum;
import com.seankrail.coursegrabber.java.Event;

import org.apache.http.client.HttpClient;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.net.ssl.HttpsURLConnection;

/**
 * Project: CourseGrabber
 * Author: Sean Krail
 * Author's Website: seankrail.com
 *
 * Modified on June 21, 2015 at 6:42 PM.
 * Created on 3/14/15
 */
public class UpdateTask extends AsyncTask<String, String, Boolean> {

    //private static final String CSVURL = "http://www.seankrail.com/assets/data/coursegrabber/mycalendar-2153.csv"; // My Spring 2015 Schedule
    //private static final String CSVURL = "http://www.seankrail.com/assets/data/coursegrabber/mycalendar-2155.csv"; // My Summer 2015 Schedule
    //private static final String CSVURL = "http://www.seankrail.com/assets/data/coursegrabber/mycalendar-2158.csv"; // My Fall 2015 Schedule
    private static final String CSVURL = "https://udapps.nss.udel.edu/registration/exportCalendar?type=CSV"; // My Fall 2015 Schedule
    //private static final String LOGINURL = "https://skrail:Impression4g@cas.nss.udel.edu/cas/login";
    //private static final String LOGINURL = "https://cas.nss.udel.edu/cas/login";
    private static final String LOGINURL = "https://cas.nss.udel.edu/cas/login?service=https%3A%2F%2Fudapps.nss.udel.edu%2Fregistration%2Fj_spring_cas_security_check";

    private UpdateActivity context;
    private ProgressDialog pd;
    private byte mode;
    private boolean forward;

    private static final String TAG = "UT";

    public UpdateTask(UpdateActivity context) {
        this.context = context;
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

        Log.i(TAG, "ProgressTask created.");
    }

    @Override
    public void onPreExecute() {
        pd.show();
        Log.i(TAG, "ProgressTask pre executed.");
    }

    @Override
    public Boolean doInBackground(String... strings) {
        long start = System.currentTimeMillis();
        long t0 = start;
        publishProgress("Creating a file for your schedule", "0");

        Log.i(TAG, "started async task");
        File userFile = new File(this.context.getFilesDir(), "schedule.csv");
        try {
            if (!userFile.exists()) {
                publishProgress("File created", "2");
                userFile.createNewFile();
                Log.i(TAG,"created file");
            } else {
                publishProgress("File already exists", "2");
                Log.i(TAG,"file already exists");
            }

            publishProgress("Accessing your schedule from the internet", "5");

            //HttpsURLConnection connection;
            URL url = new URL(LOGINURL);
            String urlParams = "";
            MyCookieStore myCookieStore = new MyCookieStore(this.context);
            CookieManager msCookieManager = new CookieManager(myCookieStore, CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(msCookieManager);
            msCookieManager.getCookieStore().removeAll();


            try {
                Log.i(TAG + ".beforeGet", "URL: " + url.toURI() + ", URLParams: " + urlParams + ", msCookieMangager: " + msCookieManager.getCookieStore().getCookies().toString());
            } catch(URISyntaxException e) {
                e.printStackTrace();
            }
            urlParams = getURLParameters(url, msCookieManager);
            try {
                Log.i(TAG + ".get/post", "URL: " + url.toURI() + ", URLParams: " + urlParams + ", msCookieMangager: " + msCookieManager.getCookieStore().getCookies().toString());
            } catch(URISyntaxException e) {
                e.printStackTrace();
            }
            String redirectLocation = postURLParameters(url, urlParams, msCookieManager);
            url = new URL(redirectLocation);
            try {
                Log.i(TAG + ".post/redirect", "URL: " + url.toURI() + ", URLParams: " + urlParams + ", msCookieMangager: " + msCookieManager.getCookieStore().getCookies().toString());
            } catch(URISyntaxException e) {
                e.printStackTrace();
            }
            redirectLocation = getURLTicket(url, msCookieManager);
            Log.i(TAG + ".getURLTicket", "Output: " + redirectLocation);
            url = new URL(redirectLocation);
            try {
                Log.i(TAG + ".redirect/csv", "URL: " + url.toURI() + ", URLParams: " + urlParams + ", msCookieMangager: " + msCookieManager.getCookieStore().getCookies().toString());
            } catch(URISyntaxException e) {
                e.printStackTrace();
            }

            getRegistration(msCookieManager);


            URL csvUrl = new URL(CSVURL);
            Log.i(TAG, "Accessing URL: " + csvUrl.getPath());

            HttpsURLConnection connection = (HttpsURLConnection) csvUrl.openConnection();

            //Log.i(TAG,"Opened connection to URL");

            if(msCookieManager.getCookieStore().getCookies().size() > 0) {
                //While joining the Cookies, use ',' or ';' as needed. Most of the server are using ';'
                connection.setRequestProperty("Cookie",
                        TextUtils.join(";", msCookieManager.getCookieStore().getCookies()));
            }

            Log.i(TAG+".time","Time to open connection to download file: " + (System.currentTimeMillis() - t0));
            t0 = System.currentTimeMillis();

            int contentLength = connection.getContentLength();
            Log.i(TAG + ".content", "Content Type: " + connection.getContentType());
            DataInputStream in = new DataInputStream(connection.getInputStream());
            Log.i(TAG,"Buffering the received stream(size="+contentLength+")");

            publishProgress("Buffering your schedule of size " + contentLength + " bytes", "7");

            byte[] buffer = new byte[contentLength];
            if (contentLength != -1) {
                in.readFully(buffer);
                in.close();
            } else {
                return false;
            }
            connection.disconnect();
            Map<URI, List<HttpCookie>> map = ((MyCookieStore) ((CookieManager) (CookieHandler.getDefault())).getCookieStore()).getMapCookies();
            Set<URI> keys = map.keySet();
            for(URI key : map.keySet()) {
                String val = "";
                boolean first = true;
                for(HttpCookie s : map.get(key)) {
                    if(first) first = false;
                    else val += " | ";
                    val += s.toString();
                }
                Log.i(TAG + ".cookies", key.toString() + ", " + val);
            }

            publishProgress("Downloading your schedule from the internet", "10");

            if (buffer.length > 0) {
                Log.i(TAG,"Have DataInputStream, now write to file");
                DataOutputStream out;
                Log.i(TAG,"Writing to file: "+userFile.getName());
                FileOutputStream fos = this.context.openFileOutput(userFile.getName(), Context.MODE_PRIVATE);
                Log.i(TAG,"Writing from buffer to new file");
                out = new DataOutputStream(fos);

                publishProgress("Downloading your schedule from the internet", "15");

                out.write(buffer);
                out.flush();
                out.close();

                Log.i(TAG, "Finished writing, CSV has been copied to the user's file.");

                publishProgress("Parsing the csv file for your schedule", "20");
                Log.i(TAG+".time","Time to download file: " + (System.currentTimeMillis() - t0));
                t0 = System.currentTimeMillis();

                //progressTask.setMessage("Deleting your old schedule");
                //progressTask.setProgress(25);

                CSVParserFileReader parser = new CSVParserFileReader(userFile);
                Log.i(TAG, "Created CSVParser");
                Log.i(TAG, "Proof of success (should read CISC275011): " + parser.getEvents().get(1).getSubject());
                int n = parser.getEvents().size();
                publishProgress("Deleting your old schedule's calendar", "30");
                Log.i(TAG+".time","Time to parse CSV file: " + (System.currentTimeMillis() - t0));
                t0 = System.currentTimeMillis();

                // Delete current 'CourseGrabber' calendars, so to not make duplicate calendar

                String[] projection = new String[]{CalendarContract.Calendars.ACCOUNT_NAME};
                Cursor calCursor = this.context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                        projection, CalendarContract.Calendars.VISIBLE + " = 1",
                        null, CalendarContract.Calendars._ID + " ASC");
                String selection = CalendarContract.Calendars.ACCOUNT_NAME + " = ? ";
                if (calCursor.moveToFirst()) {
                    String displayName = "";
                    do {
                        long id = calCursor.getLong(0);

                        String info = "";
                        for (String string : calCursor.getColumnNames()) {
                            info += string + " ";
                        }
                        Log.i(TAG + ".delete", "Column names: " + info);

                        if (calCursor.getString(0).contentEquals("CourseGrabber")) {
                            Log.i(TAG + ".delete", "Tried deleting CourseGrabber calendar");
                            this.context.getContentResolver().delete(CalendarContract.Calendars.CONTENT_URI, selection, new String[]{"CourseGrabber"});
                        } else if (calCursor.getString(0).contentEquals("Course Grabber")) {
                            Log.i(TAG + ".delete", "Tried deleting Course Grabber calendar");
                            this.context.getContentResolver().delete(CalendarContract.Calendars.CONTENT_URI, selection, new String[]{"Course Grabber"});
                        } else {
                            Log.i(TAG + ".delete", "No such calendar: CourseGrabber");
                        }
                    } while (calCursor.moveToNext());
                }
                calCursor.close();

                publishProgress("Creating a new calendar", "40");
                Log.i(TAG+".time","Time to delete old calendar: " + (System.currentTimeMillis() - t0));
                t0 = System.currentTimeMillis();

                // Now, create 'CourseGrabber' calendar

                Uri calUri = CalendarContract.Calendars.CONTENT_URI;
                ContentValues cv = new ContentValues();
                cv.put(CalendarContract.Calendars.ACCOUNT_NAME, "CourseGrabber");
                cv.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
                cv.put(CalendarContract.Calendars.NAME, "CourseGrabber");
                cv.put(CalendarContract.Calendars.OWNER_ACCOUNT, "skrail");
                cv.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "My UD Schedule");
                ColorEnum color = ColorEnum.values()[(new Random()).nextInt(ColorEnum.values().length)];
                Log.i(TAG + ".Color", "Calendar Color: " + color.getColor());
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

                Log.i(TAG, "Added a calendar with Uri result: " + result.toString());

                publishProgress("Adding your courses to the new calendar", "45");
                Log.i(TAG+".time","Time to create new calendar: " + (System.currentTimeMillis() - t0));
                t0 = System.currentTimeMillis();

                // Add id to new calendar

                projection = new String[] {CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars._ID};
                calCursor = this.context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                        projection, CalendarContract.Calendars.VISIBLE + " = 1",
                        null, CalendarContract.Calendars._ID + " ASC");
                long calendarID = -1;
                if (calCursor.moveToFirst()) {
                    String displayName = "";
                    do {
                        Log.i(TAG + ".Calendar", "Calendar Cursor: " + calCursor.getString(0));
                        if (calCursor.getString(0).contentEquals("CourseGrabber") || calCursor.getString(0).contentEquals("Course Grabber")) {
                            calendarID = calCursor.getLong(1);
                            Log.i(TAG, "Added id(" + calendarID + ") to CourseGrabber calendars");
                        } else {
                            Log.e(TAG, "No such calendar: CourseGrabber, calCursor.getString(0)=" + calCursor.getString(0));
                        }
                    } while (calCursor.moveToNext());
                }
                calCursor.close();

                Log.i(TAG+".time","Time to add id to new calendar: " + (System.currentTimeMillis() - t0));
                t0 = System.currentTimeMillis();

                // Add courses to 'CourseGrabber' calendar, THERE IS ONE AND ONLY ONE

                if (calendarID != -1) {
                    double inc = ( ((double) (25)) / ((double) (n)) );
                    Map<String, ColorEnum> uniqueSubjects = new TreeMap<String, ColorEnum>();
                    Set<ColorEnum> uniqueColors = new TreeSet<ColorEnum>();
                    for (ColorEnum ce : ColorEnum.values()) uniqueColors.add(ce);
                    ArrayList<Event> events = parser.getEvents();
                    Iterator<Event> iterator = events.iterator();
                    Log.i(TAG + ".Progress", "Size = " + events.size() + " and inc = " + inc);
                    long eventID = -1;
                    for (int i = 0; iterator.hasNext(); i++) {
                        Event e = iterator.next();
                        publishProgress("Adding " + e.getSubject() + " to the new calendar", (int) (inc * i + 51) + "");
                        cv = new ContentValues();
                        if (uniqueSubjects.containsKey(e.getSubject())) {
                            cv.put(CalendarContract.Events.EVENT_COLOR, Color.parseColor(uniqueSubjects.get(e.getSubject()).getColor()));
                        } else {
                            ColorEnum ce = (ColorEnum) uniqueColors.toArray()[(new Random()).nextInt(uniqueColors.size())];
                            cv.put(CalendarContract.Events.EVENT_COLOR, Color.parseColor(ce.getColor()));
                            uniqueSubjects.put(e.getSubject(), ce);
                            uniqueColors.remove(ce);
                            Log.i(TAG + ".Color", "Color: " + ce.getColor());
                        }
                        Log.i(TAG + ".subject=" + e.getSubject(), "EventID: " + (eventID + i + 2) + ", Subject: " + e.getSubject() + ", Start: " + e.getStart() + ", End:" + e.getEnd() + ", AllDay: " + e.getAllDay() + ", Location: " + e.getLocation() + ", CalendarID: " + calendarID + ", TimeZone: " + TimeZone.getDefault().getID() + ", AccessLevel: " + CalendarContract.Events.ACCESS_CONFIDENTIAL + ".");
                        Log.i(TAG + ".Events._ID", "" + (eventID + i + 2));
                        cv.put(CalendarContract.Events._ID, (eventID + i + 2));
                        cv.put(CalendarContract.Events.TITLE, e.getSubject());
                        cv.put(CalendarContract.Events.DTSTART, e.getStart());
                        cv.put(CalendarContract.Events.DTEND, e.getEnd());
                        cv.put(CalendarContract.Events.ALL_DAY, e.getAllDay());
                        if (e.getLocation() != null) cv.put(CalendarContract.Events.EVENT_LOCATION, e.getLocation());
                        cv.put(CalendarContract.Events.CALENDAR_ID, calendarID);
                        cv.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
                        cv.put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_CONFIDENTIAL);

                        //cv.put(CalendarContract.Events._ID, i);

                        Uri uri = this.context.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, cv);
                        cv.clear();

                        //Log.i(TAG, "Index " + i + ": Calendar_ID(" + calendarID + ")" +" Event_ID(" + (eventID + i + 2) + ")");
                    }

                    publishProgress("Adding reminders to your new calendar", "75");
                    Log.i(TAG+".time","Time to add 164 classes to the calendar: " + (System.currentTimeMillis() - t0));
                    t0 = System.currentTimeMillis();

                    boolean reminders = context.getSharedPreferences(context.getString(R.string.preference_file_key), context.MODE_PRIVATE).getBoolean("reminders", true);

                    if (reminders) {
                        inc = ( ((double) (25)) / ((double) (n)) );
                        int i = 0;

                        projection = new String[]{CalendarContract.Events.CALENDAR_ID, CalendarContract.Events._ID, CalendarContract.Events.ALL_DAY, CalendarContract.Events.TITLE};
                        Cursor eventCursor = this.context.getContentResolver().query(CalendarContract.Events.CONTENT_URI,
                                projection, CalendarContract.Events.VISIBLE + " = 1",
                                null, CalendarContract.Events._ID + " ASC");

                        // Get user's preference
                        int reminder = context.getSharedPreferences(context.getString(R.string.preference_file_key), context.MODE_PRIVATE).getInt("reminder", 30);

                        if (eventCursor.moveToFirst()) {
                            String displayName = "";
                            do {
                                Log.i(TAG, "Title: '" + eventCursor.getString(3) + "' All Day: '" + eventCursor.getString(2) + "'");
                                if (eventCursor.getLong(0) == calendarID && eventCursor.getString(2).contentEquals("0")) {
                                    publishProgress("Adding reminders to your new calendar", (int) (inc * i + 75) + "");
                                    eventID = eventCursor.getLong(1);
                                    Log.i(TAG + ".event", "Added event id(" + eventID + ")");
                                    cv = new ContentValues();
                                    cv.put(CalendarContract.Reminders.EVENT_ID, eventID);
                                    cv.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                                    cv.put(CalendarContract.Reminders.MINUTES, reminder);
                                    Uri uri = this.context.getContentResolver().insert(CalendarContract.Reminders.CONTENT_URI, cv);
                                    cv.clear();
                                    Log.i(TAG, "Reminder added.");
                                    i++;
                                } else if (eventCursor.getString(2).contentEquals("1")) {
                                    Log.i(TAG, "Skip reminder for all-day event: '" + eventCursor.getLong(3) + "'");
                                    publishProgress("Skipping a reminder for an all-day event", (int) (inc * i + 75) + "");
                                } else {
                                    publishProgress("ERROR: \"No such calendar.\"", (int) (inc * i + 75) + "");
                                    Log.e(TAG, "No such calendar: '" + eventCursor.getLong(3) + "'");
                                }
                            } while (eventCursor.moveToNext());
                        }
                        eventCursor.close();
                    }
                    publishProgress("Finished uploading your courses to the new calendar", "100");
                    Log.i(TAG+".time","Time to add reminders: " + (System.currentTimeMillis() - t0));
                    Log.i(TAG+".time", "Total Time: " + (System.currentTimeMillis() - start));
                    return Boolean.TRUE;    // success
                }
            }
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
            Log.i(TAG, "ProgressTask post executed.");
        } else {
            Toast.makeText(this.context, "Error occurred, no such calendar exists", Toast.LENGTH_LONG).show();
            Log.i(TAG, "ProgressTask failed to execute");
        }
        this.context.updateUI();
        pd.dismiss();
    }

    // End of UpdateTask's implemented functions
    // Start of subsidiary functions

    private String getURLParameters(URL url, CookieManager msCookieManager) {

        // http://developer.android.com/reference/java/net/HttpURLConnection.html

        // http://stackoverflow.com/a/16171708
        //final String COOKIES_HEADER = "Set-Cookie";
        try {
            // 1. Start HTTPURLConnection by calling URL.openConnection()
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            msCookieManager = new CookieManager();

            try {
                connection.setRequestMethod("GET");
                connection.setUseCaches(false);
                //connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                //connection.setRequestProperty("Accept-Charset", "UTF-8");
                //connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                //connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
                //connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                //connection.setRequestProperty("Connection", "keep-alive");
                //connection.setRequestProperty("Host", "cas.nss.udel.edu");
                //connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:40.0) Gecko/20100101 Firefox/40.0");
            } catch (ProtocolException e) {
                Log.e(TAG + ".getURLParams", "Error: ProtocolException");
                e.printStackTrace();
            }

            connection.setUseCaches(false);
            connection.setDoInput(true);    // read response headers
            connection.setDoOutput(false);  // only send headers


            // Proves I am getting the right page
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                Log.i(TAG + ".test", "url.getPath(): " + url.getPath() + ", connection.getURL().getPath(): " + connection.getURL().getPath());

                String inputLine;
                StringBuffer html = new StringBuffer();


                List<Pair<String, String>> keyValuePairs = new ArrayList<>();
                keyValuePairs.add(new Pair<>("udelnetid", "skrail"));
                keyValuePairs.add(new Pair<>("pword", "Impression4g"));
                keyValuePairs.add(new Pair<>("action", ""));
                keyValuePairs.add(new Pair<>("udid", ""));
                keyValuePairs.add(new Pair<>("pin", ""));

                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.contains("<input type=\"hidden\" ")) {
                        String[] splitInputLine = inputLine.split("\\s+|/>|<");
                        String key = "", value = "";
                        for (String splitLine : splitInputLine) {
                            if (splitLine.contains("name=")) {
                                key = splitLine.split("=")[1];
                                key = key.substring(1, key.length() - 1);
                                switch (key) {
                                    case "username":
                                        value = "skrail";
                                        break;
                                    case "password":
                                        value = "Impression4g";
                                        break;
                                }
                            } else if (splitLine.contains("value=")) {
                                value = splitLine.split("=")[1];
                                value = value.substring(1, value.length() - 1);
                            }
                        }
                        Log.i(TAG + ".kvInput", inputLine);
                        keyValuePairs.add(new Pair<String, String>(key, value));
                    }
                    html.append(inputLine + "\n");
                }
                in.close();

                for(Pair<String, String> pair : keyValuePairs) {
                    Log.i(TAG + ".keyValue", pair.first + ", " + pair.second);
                }

                //System.out.println("URL1 Content... \n" + html.toString());

                String urlParams = "";
                boolean first = true;
                for (Pair<String, String> pair : keyValuePairs) {
                    if (first) first = false;
                    else urlParams += "&";

                    urlParams += pair.first + "=" + URLEncoder.encode(pair.second, "UTF-8");
                }
                Log.i(TAG + ".urlParams", urlParams);


                try {
                    Log.i(TAG + ".connection", "Response Code/Message: " + connection.getResponseCode() + ", \"" + connection.getResponseMessage() + "\"");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Read response

                Log.i(TAG + ".connection", "Redirect Url?: " + connection.getURL().toString());

                Map<String, List<String>> headerFields = connection.getHeaderFields();
                Log.i(TAG + ".connection", "HeaderFields: " + headerFields.toString());
                List<String> cookiesHeader = headerFields.get("Set-Cookie");
                List<String> locationHeader = headerFields.get("Location");


                if (cookiesHeader != null) {
                    for (String cookie : cookiesHeader) {
                        Log.i(TAG + ".cookie", cookie);
                        msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                    }
                }

                if (locationHeader != null) {
                    for (String location : locationHeader) {
                        Log.i(TAG + ".location", location);
                        //msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                        //loginUrl = new URL(location);
                    }
                } else Log.i(TAG + ".location", "No Location in response header");

                connection.disconnect();

                return urlParams;

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Exceptions have been thrown
        return null;
    }

    private String postURLParameters(URL url, String urlParams, CookieManager msCookieManager) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            // http://stackoverflow.com/a/16171708

            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Host", "cas.nss.udel.edu");
            connection.setRequestProperty("Referer", "https://cas.nss.udel.edu/cas/login?service=https%3A%2F%2Fudapps.nss.udel.edu%2Fregistration%2Fj_spring_cas_security_check");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParams.getBytes().length));
            Log.i(TAG + ".contentLen", Integer.toString(urlParams.getBytes().length));
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                //While joining the Cookies, use ',' or ';' as needed. Most of the server are using ';'
                connection.setRequestProperty("Cookie", TextUtils.join(";", msCookieManager.getCookieStore().getCookies()));
            }

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            connection.setFixedLengthStreamingMode(urlParams.getBytes().length);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParams);
            wr.flush();
            wr.close();


            String location = connection.getHeaderField("Location");
            Log.i(TAG + ".loc", "Post1 Response: " + location);

            // end of first post request
            // start of second post request

            connection = (HttpsURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            // http://stackoverflow.com/a/16171708

            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Host", "cas.nss.udel.edu");
            connection.setRequestProperty("Referer", "https://cas.nss.udel.edu/cas/login?service=https%3A%2F%2Fudapps.nss.udel.edu%2Fregistration%2Fj_spring_cas_security_check");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParams.getBytes().length));
            Log.i(TAG + ".contentLen", Integer.toString(urlParams.getBytes().length));
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                //While joining the Cookies, use ',' or ';' as needed. Most of the server are using ';'
                connection.setRequestProperty("Cookie", TextUtils.join(";", msCookieManager.getCookieStore().getCookies()));
            }

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            connection.setFixedLengthStreamingMode(urlParams.getBytes().length);

            wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParams);
            wr.flush();
            wr.close();

            try {
                connection.getInputStream();
            } catch (ProtocolException e) {
                e.printStackTrace();
                // too many redirects
                connection.disconnect();
            }

            return location;

        } catch(IOException e) {
            e.printStackTrace();
        }
        // Exceptions have been thrown
        return null;
    }

    private String getURLTicket(URL url, CookieManager msCookieManager) {

        // http://developer.android.com/reference/java/net/HttpURLConnection.html

        // http://stackoverflow.com/a/16171708
        try {
            // 1. Start HTTPURLConnection by calling URL.openConnection()
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            msCookieManager = new CookieManager();

            try {
                connection.setRequestMethod("GET");
                connection.setUseCaches(false);
                //connection.setRequestProperty("Accept", "*/*");
                //connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
                //connection.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
                //connection.setRequestProperty("Accept-Charset", "UTF-8");
                //connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("Host", "udapps.nss.udel.edu");
                try {
                    Log.i(TAG + ".getURLTicket", "Referer: " + url.toURI().toASCIIString());
                    connection.setRequestProperty("Referer", url.toURI().toASCIIString());
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                    //While joining the Cookies, use ',' or ';' as needed. Most of the server are using ';'
                    connection.setRequestProperty("Cookie", TextUtils.join(";", msCookieManager.getCookieStore().getCookies()));
                }
            } catch (ProtocolException e) {
                Log.e(TAG + ".getURLParams", "Error: ProtocolException");
                e.printStackTrace();
            }

            connection.setUseCaches(false);
            connection.setDoInput(true);    // read response headers
            connection.setDoOutput(false);  // only send headers

            Log.i(TAG + ".getTicket", "Ticket: " + connection.getResponseMessage() + ", " + connection.getResponseCode());

            String location = connection.getHeaderField("Location");

            return location;

        } catch (IOException e) {
            e.printStackTrace();
        }
        // Exceptions have been thrown
        return null;
    }

    private void getRegistration(CookieManager msCookieManager) {
        try {
            // 1. Start HTTPURLConnection by calling URL.openConnection()
            URL url = new URL("https://udapps.nss.udel.edu/registration/");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            msCookieManager = new CookieManager();

            try {
                connection.setRequestMethod("GET");
                connection.setUseCaches(false);
                //connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                //connection.setRequestProperty("Accept-Charset", "UTF-8");
                //connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                //connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
                //connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                //connection.setRequestProperty("Connection", "keep-alive");
                //connection.setRequestProperty("Host", "cas.nss.udel.edu");
                //connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:40.0) Gecko/20100101 Firefox/40.0");
                if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                    //While joining the Cookies, use ',' or ';' as needed. Most of the server are using ';'
                    connection.setRequestProperty("Cookie", TextUtils.join(";", msCookieManager.getCookieStore().getCookies()));
                }
            } catch (ProtocolException e) {
                Log.e(TAG + ".getURLParams", "Error: ProtocolException");
                e.printStackTrace();
            }

            connection.setUseCaches(false);
            connection.setDoInput(true);    // read response headers
            connection.setDoOutput(false);  // only send headers

            String location = connection.getHeaderField("Location");

            Log.i(TAG + ".getRegistration()", "Location: " + location);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
