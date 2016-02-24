package com.seankrail.coursegrabber.java;

/**
 * Project: CourseGrabber
 * Author: Sean Krail
 * Author's Website: seankrail.com
 * <p>
 * Created on February 22, 2016 at 5:26 PM.
 */

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;


public class GrabberDebug {

    private static final String TAG = "GRABBER";

    // CONSTANT inputs
    public static String LOGIN_URL = "https://cas.nss.udel.edu/cas/login?service=https%3A%2F%2Fudapps.nss.udel.edu%2Fregistration%2Fj_spring_cas_security_check";
    public static String CSV_URL = "https://udapps.nss.udel.edu/registration/exportCalendar?type=CSV";
    public static String ICAL_URL = "https://udapps.nss.udel.edu/registration/exportCalendar?type=ICAL";

    // CONSTANT outputs
    public static int SUCCESS = 0;			// SUCCESS
    public static int ERR_CONNECTION = 1;	// Failed to load webpage (probably not connect to internet)
    public static int ERR_LOGIN = 2;		// Failed to post login info (invalid login info)
    public static int ERR_REDIRECT_1 = 3;	// Failed 1st redirect (my code is wrong)
    public static int ERR_REDIRECT_2 = 4;	// Failed 2nd redirect (my code is wrong)
    public static int ERR_REDIRECT_3 = 5;	// Failed 3rd redirect (my code is wrong)
    public static int ERR_REDIRECT_4 = 6;	// Failed 4th redirect (my code is wrong)
    public static int ERR_REDIRECT_5 = 7;	// Failed 5th and final redirect (my code is wrong)
    public static int ERR_DOWNLOAD = 8;		// Failed to download course info (my code is wrong)

    public static int grab(Context context, File file, String username, String password, boolean csv) {
        Log.i(TAG, "Initiated course grab. w/ " + username + ":" + password);
        HttpsURLConnection.setFollowRedirects(false);
        String[] urlParamsAndCookie;
        if ((urlParamsAndCookie = GrabberDebug.initialGet(username, password)) != null) {
            String[] locationAndCookie;
            if ((locationAndCookie = GrabberDebug.initialPost(urlParamsAndCookie[0], urlParamsAndCookie[1])) != null) {
                if ((locationAndCookie = GrabberDebug.redirect(locationAndCookie[0], locationAndCookie[1], 1)) != null) {
                    if ((locationAndCookie = GrabberDebug.redirect(locationAndCookie[0], locationAndCookie[1], 2)) != null) {
                        if ((locationAndCookie = GrabberDebug.redirect(locationAndCookie[0], locationAndCookie[1], 3)) != null) {
                            if ((locationAndCookie = GrabberDebug.redirect(locationAndCookie[0], locationAndCookie[1], 4)) != null) {
                                String cookie = locationAndCookie[1];
                                if ((locationAndCookie = GrabberDebug.redirect(locationAndCookie[0], locationAndCookie[1], 5)) != null) {
                                    if (getSchedule(context, file, cookie, csv)) return SUCCESS; // SUCCESS
                                    else return ERR_DOWNLOAD; // Failed to download course info (my code is wrong)
                                }
                                else if (getSchedule(context, file, cookie, csv)) return SUCCESS; // SUCCESS
                                else return ERR_DOWNLOAD; // Failed to download course info (my code is wrong)
                            } else return ERR_REDIRECT_4; // Failed 4th redirect (my code is wrong)
                        } else return ERR_REDIRECT_3; // Failed 3rd redirect (my code is wrong)
                    } else return ERR_REDIRECT_2; // Failed 2nd redirect (my code is wrong)
                } else return ERR_REDIRECT_1; // Failed 1st redirect (my code is wrong)
            } else return ERR_LOGIN; // Failed to post login info (invalid login info)
        } else return ERR_CONNECTION; // Failed to load webpage (probably not connect to internet)
    }


    private static String[] initialGet(String username, String password) {
        String[] urlParamsAndCookie = new String[2];
        ArrayList<Pair<String, String>> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new Pair<String, String>("udelnetid", username));
        keyValuePairs.add(new Pair<String, String>("pword", password));
        keyValuePairs.add(new Pair<String, String>("action", ""));
        keyValuePairs.add(new Pair<String, String>("udid", ""));
        keyValuePairs.add(new Pair<String, String>("pin", ""));
        keyValuePairs.add(new Pair<String, String>("username", username));
        keyValuePairs.add(new Pair<String, String>("password", password));
        try {
            URL url = new URL(LOGIN_URL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.addRequestProperty("User-Agent", "CourseGrabber");

            conn.connect();

            // READ response body for JSESSIONID info
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("<input type=\"hidden\" ")) {
                    if (inputLine.contains("name=\"lt\"")) {
                        keyValuePairs.add(new Pair<String, String>("lt", inputLine.split("\"")[5]));
                        System.out.println("Found lt value of '" + inputLine.split("\"")[5] + "'");
                    } else if (inputLine.contains("name=\"execution\"")) {
                        keyValuePairs.add(new Pair<String, String>("execution", inputLine.split("\"")[5]));
                        System.out.println("Found execution value of '" + inputLine.split("\"")[5] + "'");
                    }
                }
            }
            in.close();
            keyValuePairs.add(new Pair<String, String>("_eventId", "submit"));

            System.out.println("\nGET");
            System.out.println("Number of Set-Cookies:\t'" + conn.getHeaderFields().get("Set-Cookie").size() + "'");
            System.out.println("Set-Cookie:\t\t'" + conn.getHeaderFields().get("Set-Cookie").get(0) + "'");
            urlParamsAndCookie[1] = conn.getHeaderFields().get("Set-Cookie").get(0).split(";")[0];
            for (int i = 1; i < conn.getHeaderFields().get("Set-Cookie").size(); i++) {
                System.out.println("Set-Cookie:\t\t'" + conn.getHeaderFields().get("Set-Cookie").get(i) + "'");
                urlParamsAndCookie[1] += "; " + conn.getHeaderFields().get("Set-Cookie").get(i).split(";")[0];
            }
            System.out.println("Set-Cookie(Out):\t'" + urlParamsAndCookie[1] + "'");
            conn.disconnect();

            // EXTRACT URL parameters for initial POST request
            urlParamsAndCookie[0] = "";
            boolean first = true;
            for (Pair<String, String> pair : keyValuePairs) {
                if (first) first = false;
                else urlParamsAndCookie[0] += "&";
                urlParamsAndCookie[0] += pair.first + "=" + URLEncoder.encode(pair.second, "UTF-8");
            }
            System.out.println("URL Parameters:\t\t'" + urlParamsAndCookie[0] + "'");
            return urlParamsAndCookie;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private static String[] initialPost(String urlParams, String cookie) {
        String[] locationAndCookie = new String[3];
        try {
            URL url = new URL(LOGIN_URL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            conn.addRequestProperty("Cookie", cookie);
            conn.addRequestProperty("User-Agent", "CourseGrabber");
            conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.addRequestProperty("Content-Length", "" + urlParams.length());
            System.out.println("\nPOST");
            System.out.println("Cookie:\t\t\t'" + cookie + "'");

            conn.connect();

            // Write to output
            PrintWriter out = new PrintWriter(conn.getOutputStream());
            out.write(urlParams);
            out.close();


            //System.out.println(conn.getHeaderFields());
            System.out.println("Response Code:\t\t'" + conn.getResponseCode() + "'");
            System.out.println("Location:\t\t'" + conn.getHeaderField("Location") + "'");
            //System.out.println("Number of Set-Cookies:\t'" + conn.getHeaderFields().get("Set-Cookie").size() + "'");
            //for (int i = 0; i < conn.getHeaderFields().get("Set-Cookie").size(); i++) {
            //	System.out.println("Set-Cookie:\t\t'" + conn.getHeaderFields().get("Set-Cookie").get(i) + "'");
            //}
            locationAndCookie[0] = conn.getHeaderField("Location");
            locationAndCookie[1] = cookie ;//+ "; " + conn.getHeaderFields().get("Set-Cookie").get(0).split(";")[0];
            //System.out.println("Set-Cookie(Out):\t'" + locationAndCookie[1] + "'");

            // GET response code
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            if (responseCode == HttpsURLConnection.HTTP_MOVED_TEMP) return locationAndCookie;
            else return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private static String[] redirect(String location, String cookie, int num) {
        String[] locationAndCookies = new String[]{location, cookie};
        try {
            URL url = new URL(location);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            System.out.println("\nREDIRECT #" + num);
            conn.addRequestProperty("User-Agent", "CourseGrabber");
            if (cookie != null && num != 1) conn.addRequestProperty("Cookie", cookie);
            if (cookie != null && num != 1)
                System.out.println("Cookie:\t\t\t'" + cookie + "'");

            conn.connect();


            //System.out.println(conn.getHeaderFields());
            System.out.println("Response Code:\t\t'" + conn.getResponseCode() + "'");
            System.out.println("Location:\t\t'" + conn.getHeaderField("Location") + "'");
            locationAndCookies[0] = conn.getHeaderField("Location");
            if (conn.getHeaderFields().get("Set-Cookie") != null && num == 1) {
                System.out.println("Number of Set-Cookies:\t'" + conn.getHeaderFields().get("Set-Cookie").size() + "'");
                locationAndCookies[1] = conn.getHeaderFields().get("Set-Cookie").get(0).split(";")[0];
                for (int i = 1; i < conn.getHeaderFields().get("Set-Cookie").size(); i++) {
                    System.out.println("Set-Cookie:\t\t'" + conn.getHeaderFields().get("Set-Cookie").get(i) + "'");
                    locationAndCookies[1] += "; " + conn.getHeaderFields().get("Set-Cookie").get(i).split(";")[0];
                }
                System.out.println("Set-Cookie(Out):\t'" + locationAndCookies[1] + "'");
            }

            // GET response code
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            if (responseCode == HttpsURLConnection.HTTP_MOVED_TEMP) return locationAndCookies;
            else return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean getSchedule(Context context, File file, String cookie, boolean csv) {
        String location = csv ? CSV_URL : ICAL_URL;
        URL url;
        try {
            Log.i(TAG, "getSchedule() called.");
            url = new URL(location);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.addRequestProperty("User-Agent", "CourseGrabber");
            conn.addRequestProperty("Cookie", cookie);

            conn.connect();

            Log.i(TAG, "getSchedule(): ResponseCode=" + conn.getResponseCode());
            Log.i(TAG, "getSchedule(): ContentLength=" + conn.getContentLength());
            Log.i(TAG, "getSchedule(): ContentType=" + conn.getContentType());
            Log.i(TAG, "getSchedule(): HeaderFields=" + conn.getHeaderFields());
            int contentLength = conn.getContentLength();
            DataInputStream in = new DataInputStream(conn.getInputStream());

            DataOutputStream out;
            FileOutputStream fos = context.openFileOutput(file.getName(), Context.MODE_PRIVATE);
            out = new DataOutputStream(fos);

            byte buffer;
            try {
                while ((buffer = in.readByte()) != -1) out.writeByte(buffer);
            } catch (IOException e) {
                // EOF
            }

            //if (contentLength == -1) return false;
            //byte[] buffer = new byte[contentLength];
            //if (buffer.length <= 0) return false;
            //in.readFully(buffer);
            in.close();

            //DataOutputStream out;
            //FileOutputStream fos = context.openFileOutput(file.getName(), Context.MODE_PRIVATE);
            //out = new DataOutputStream(fos);

            //out.write(buffer);
            out.flush();
            out.close();

            conn.disconnect();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}

