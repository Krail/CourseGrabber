package com.seankrail.coursegrabber.android;

import android.content.Context;
import android.content.Intent;
import android.widget.CheckedTextView;

/**
 * Project: CourseGrabber
 * Package: ${PACKAGE_NAME}
 *
 * Created by Sean Krail on June 21, 2015 at 6:37 PM.
 */
public class CourseGrabberApplication extends android.app.Application {

    @Override
    public void onCreate() {
        // do nothing, calls UpdateActivity's onCreate method after this
        super.onCreate();
    }
}
