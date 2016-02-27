package com.seankrail.coursegrabber.java;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Krail on 3/12/15.
 */
public class Event {
    private String subject;
    private Long start;
    private Long end;
    private byte allDay;
    private String location;

    private static final String TAG = "Event";

    public Event(String subject, String startD, String startT, String endD, String endT, String allDay, String location) {
        this.subject = subject;
        try {
            this.start = (new SimpleDateFormat("M/d/y h:m a", Locale.ENGLISH).parse(startD + " " + startT)).getTime();
            this.end = (new SimpleDateFormat("M/d/y h:m a",Locale.ENGLISH).parse(endD + " " + endT)).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (allDay.contentEquals("True")) this.allDay = 1;
        else this.allDay = 0;


        // Locations are in the format 'MEM106' or 'MEM106A'
        if (location.length() == 6 || location.length() == 7) {
            //Log.i(TAG, "Location: '" + location + "' Building: '" + location.substring(0, 3) + "'");
            BuildingEnum be = null;
            for (BuildingEnum b : BuildingEnum.values()) {if (b.name().contentEquals(location.substring(0, 3))) be = b;}
            if (be == null) {
                //Log.i(TAG, "Invalid BuildingEnum: " + location.substring(0, 3));
                this.location = location;
            } else if (location.length() == 6) {
                Classroom classroom = new Classroom(new Building(be), Short.parseShort(location.substring(3, 6)), '~');
                this.location = classroom.toString();
            } else if (location.length() == 7) {
                Classroom classroom = new Classroom(new Building(be), Short.parseShort(location.substring(3, 6)), location.charAt(6));
                this.location = classroom.toString();
            } else this.location = location;
        } else this.location = null;
    }

    public String getSubject() {return this.subject;}
    public Long getStart() {return this.start;}
    public Long getEnd() {return this.end;}
    public byte getAllDay() {return this.allDay;}
    public String getLocation() {return this.location;}
}
