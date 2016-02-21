# CourseGrabber #

  CourseGrabber is an Android application which implements Android's
[Calendar Contract API]
(http://developer.android.com/reference/android/provider/CalendarContract.html)
. University of Delaware students can use the app to download their schedule from UD's UDSIS server, then view it in their favorite calendar app. The trickest part of this project was navigating UD's Central Authentication Service through HTTP GET and POST requests, which used JSESSIONID cookies and many 302 redirects.


# ToDo #

  Do the actual course grabbing within the app. For now, I do it from my laptop in a
separate Node.js program.
