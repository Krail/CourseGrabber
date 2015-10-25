package com.seankrail.coursegrabber.java;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Krail on 3/12/15.
 */
public class CSVParserFileInputStream {
    private File csv;
    private ArrayList<Event> events;

    private static final String TAG = "CSVParser";

    public CSVParserFileInputStream(File csv) {
        this.csv = csv;
        this.events = new ArrayList<Event>();
        this.parse();
    }

    private void parse() {
        try {
            FileInputStream fis = new FileInputStream(csv);
            byte[] data = new byte[(int) csv.length()];
            fis.read(data);
            fis.close();
            String[] eventStrings  = data.toString().split("\n");
            String[] entities;
            for (String s : eventStrings) {
                entities = s.split(",");
                //                       (  subject  , start date , start time ,  end date  ,  end time  ,   all day  ,  location  )
                this.events.add(new Event(entities[0], entities[1], entities[2], entities[3], entities[4], entities[5], entities[6]));
                Log.i(TAG, "Wrote event from line: " + s);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Event> getEvents() {return this.events;}
}
