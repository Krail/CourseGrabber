package com.seankrail.coursegrabber.java;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Krail on 3/12/15.
 */
public class CSVParserFileReader {
    private File csv;
    private ArrayList<Event> events;

    private static final String TAG = "CSVParser";

    public CSVParserFileReader(File csv) {
        this.csv = csv;
        this.events = new ArrayList<Event>();
        this.parse();
    }

    private void parse() {
        BufferedReader br = null;
        this.events = new ArrayList<Event>();
        String line = "";
        String split = ",";
        try {
            br = new BufferedReader(new FileReader(csv));
            line = br.readLine();
            //Log.i(TAG + ".CSVParser", "Line: " + line);
            while ((line = br.readLine()) != null) {
                String[] entities = line.split(split);
                //                       (  subject  , start date , start time ,  end date  ,  end time  ,   all day  ,  location  )
                this.events.add(new Event(entities[0], entities[1], entities[2], entities[3], entities[4], entities[5], entities[6]));
                //Log.i(TAG, "Wrote event from line: "+line);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ArrayList<Event> getEvents() {return this.events;}
}
