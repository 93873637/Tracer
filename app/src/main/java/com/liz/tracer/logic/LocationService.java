package com.liz.tracer.logic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.liz.androidutils.LocationUtils;
import com.liz.androidutils.LogEx;
import com.liz.androidutils.LogUtils;
import com.liz.androidutils.NumUtils;
import com.liz.androidutils.TimeUtils;
import com.liz.tracer.app.MyApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.liz.androidutils.LocationUtils.differentLocation;

@SuppressLint({"MissingPermission"})
@SuppressWarnings("unused, WeakerAccess")
public class LocationService {

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private static LocationService inst_ = new LocationService();

    public static LocationService inst() {
        return inst_;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private LocationManager mLocationManager = null;
    private String mLocationProvider = "";
    private LocationCallback mLocationCallback = null;
    private ArrayList<LocationCallback> mCallbackList = new ArrayList<>();

    // running parameters
    private ArrayList<Location> mLocationList = new ArrayList<>();  // list of locations on change
    private long mTimeStart = 0;
    private long mTimeStop = 0;
    private Location mLocationMax = null;
    private float mMaxSpeedAccuracy = 0;
    private boolean mIsRunning = false;

    public interface LocationCallback {
        void onLocationUpdate();
    }

    public void init(Context context) {
        if (!checkPermissions(context)) {
            LogEx.e("No location permissions");
            return;
        }

        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager == null) {
            LogEx.e("get location manager failed");
            return;
        }

//        Criteria criteria = new Criteria();
//        criteria.setAccuracy(Criteria.ACCURACY_FINE);
//        criteria.setAltitudeRequired(false);
//        criteria.setBearingRequired(false);
//        criteria.setCostAllowed(true);
//        criteria.setPowerRequirement(Criteria.POWER_LOW);
//        mLocationProvider = mLocationManager.getBestProvider(criteria, true);

        List<String> providerList = mLocationManager.getProviders(true);
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            mLocationProvider = LocationManager.GPS_PROVIDER;
        } else if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
            mLocationProvider = LocationManager.NETWORK_PROVIDER;
        } else {
            Toast.makeText(context, "No location mLocationProvider", Toast.LENGTH_SHORT).show();
            LogEx.e("No location mLocationProvider available");
        }
    }

    public void setLocationCallback(LocationCallback callback) {
        mLocationCallback = callback;
    }

    public void addLocationCallback(LocationCallback callback) {
        mCallbackList.add(callback);
    }

    public void release() {
        onStop();
    }

    public void switchTracing() {
        if (mIsRunning) {
            onStop();
        } else {
            onStart();
        }
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public Location getValidBearingLocation() {
        return LocationUtils.getValidBearingLocation(mLocationList);
    }

    public float getValidBearing() {
        return LocationUtils.getValidBearing(mLocationList);
    }

    public String getValidBearingText() {
        Location loc = LocationUtils.getValidBearingLocation(mLocationList);
        if (loc == null) {
            return "NA";
        }
        return LocationUtils.getBearingName(loc.getBearing());
    }

    public int getBearing() {
        Location location = getLastLocation();
        if (location == null) {
            return 0;
        } else {
            return (int) location.getBearing();
        }
    }

    public String getBearingText() {
        String text = "NA";
        Location location = getLastLocation();
        if (location != null) {
            if (Math.abs(location.getSpeed()) > ComDef.ZERO_SPEED) {
                text = "" + (int) location.getBearing();
                text += " " + LocationUtils.getBearingName(location.getBearing());
            }
        }
        return text;
    }

    public String getStartTimeText() {
        if (mTimeStart == 0) {
            return ComDef.TIME_RESET_STRING;
        } else {
            return TimeUtils.formatTime(mTimeStart);
        }
    }

    public double getAverageSpeed() {
        long duration = getDuration();
        if (duration == 0) {
            return 0;  //just start or param reset
        } else {
            return DataLogic.inst().getDistanceTotal() * 1000 / duration;
        }
    }

    public String getAverageSpeedText() {
        return LocationUtils.getDualSpeedText(getAverageSpeed());
    }

    public double getMaxSpeed() {
        return (mLocationMax == null) ? 0 : mLocationMax.getSpeed();
    }

    public float getMaxSpeedAccuracy() {
        return (mLocationMax == null) ? 0 : mLocationMax.getAccuracy();
    }

    public String getMaxSpeedText() {
        return LocationUtils.getDualSpeedText(getMaxSpeed()) + "/" + NumUtils.format(getMaxSpeedAccuracy(), 1);
    }

    public String getMaxDualSpeedText() {
        return LocationUtils.getDualSpeedText(getMaxSpeed());
    }

    public double getSpeedRatio(double speed) {
        double speedH = getSpeedLimitHigh(speed);
        double speedL = getSpeedLimitLow(speed);
        double range = speedH - speedL;
        if (range > 0) {
            return (speed - speedL) / range;
        }
        else {
            LogUtils.te("no speed range.");
            return 0;
        }
    }

    public double getCurrentSpeedRatio() {
        return getSpeedRatio(getCurrentSpeed());
    }

    public double getAverageSpeedRatio() {
        return getSpeedRatio(getAverageSpeed());
    }

    public double getSpeedLimitHigh(double speed) {
        for (SpeedColor sc : ComDef.SPEED_BAR_COLORS) {
            if (speed < sc.threshold) {
                return sc.threshold;
            }
        }
        return getMaxSpeed();  // speed out of range, using current max as limit
    }

    public double getSpeedLimitLow(double speed) {
        for (int i = ComDef.SPEED_BAR_COLORS.length - 1; i >= 0; i--) {
            if (speed >= ComDef.SPEED_BAR_COLORS[i].threshold) {
                return ComDef.SPEED_BAR_COLORS[i].threshold;
            }
        }
        return 0;
    }

    public String getLocationInfo() {
        return "" + mLocationList.size();
    }

    public double getCurrentSpeed() {
        if (!mIsRunning) {
            return 0;
        } else {
            Location location = getLastLocation();
            if (location == null) {
                return 0;
            } else {
                return location.getSpeed();
            }
        }
    }

    public float getCurrentAccuracy() {
        if (!mIsRunning) {
            return 0;
        } else {
            Location location = getLastLocation();
            if (location == null) {
                return 0;
            } else {
                return location.getAccuracy();
            }
        }
    }

    public boolean hasSpeed() {
        return getCurrentSpeed() > ComDef.ZERO_SPEED;
    }

    public String getCurrentSpeedText() {
        return LocationUtils.getDualSpeedText(getCurrentSpeed()) + "/" + String.format("%.1f", getCurrentAccuracy());
    }

    public String getCurrentSpeedText(boolean withAccuracy) {
        String txt = LocationUtils.getDualSpeedText(getCurrentSpeed());
        if (withAccuracy) {
            txt += "/" + String.format("%.1f", getCurrentAccuracy());
        }
        return txt;
    }

    public String getStatisInfo() {
        return "<b><font color='red'>" + DataLogic.inst().getDistanceTotalText() + "</font></b>"
                + "<br>"
                + "<b><font color='#00aa88'>" + LocationService.inst().getDurationText() + " </font></b>"
                + "<br>"
                + "<b><font color='#00ff00'>" + LocationService.inst().getMaxSpeedText() + "</font></b>"
                + "<br>"
                + "<b><font color='#8080ff'>" + LocationService.inst().getAverageSpeedText() + "</font></b>"
                + "<br>"
                + "<b><font color='red'>" + LocationService.inst().getCurrentSpeedText() + "</font></b>"
                + "<br>"
                + "<b><font color='#88ee00'>" + LocationService.inst().getValidBearingText() + "</font></b>"
                ;
    }

    public long getDuration() {
        if (mIsRunning) {
            return System.currentTimeMillis() - mTimeStart;
        } else {
            return mTimeStop - mTimeStart;
        }
    }

    public String getDurationText() {
        return (getDuration() / 1000) + "s";
    }

    public String getDurationTextFormat() {
        return TimeUtils.formatDuration(getDuration());
    }

    public String getDurationComboText() {
        return getDurationTextFormat() + "/" + getDurationText();
    }

    public String getLastLocationInfo() {
        Location location = getLastLocation();
        if (location == null) {
            return "location null";
        } else {
            return LocationUtils.getString(location);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Check Timer
    private Timer mCheckTimer;

    private void setCheckTimer(long timerDelay, long timerPeriod) {
        this.mCheckTimer = new Timer();
        this.mCheckTimer.schedule(new TimerTask() {
            public void run() {
                onCheckTimer();
            }
        }, timerDelay, timerPeriod);
    }

    private void removeCheckTimer() {
        if (this.mCheckTimer != null) {
            this.mCheckTimer.cancel();
            this.mCheckTimer = null;
        }
    }
    // Check Timer
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void resetRunningParameters() {
        mLocationList.clear();
        mTimeStart = 0;
        mTimeStop = 0;
        mLocationMax = null;
        DataLogic.inst().resetRunningParameters();
    }

    private void onStart() {
        if (mIsRunning) {
            LogUtils.td("already started");
        } else {
            mIsRunning = true;
            resetRunningParameters();
            mTimeStart = System.currentTimeMillis();
            if (TestData.testTrack()) {
                //TestData.startTestTrackingOnTimer();
                TestData.startTestTracking();
            } else if (TestData.testLoadAll()) {
                TestData.startTestLoadAll();
            } else {
                mLocationManager.requestLocationUpdates(mLocationProvider,
                        ComDef.LOCATION_UPDATE_MIN_TIME,
                        ComDef.LOCATION_UPDATE_MIN_DISTANCE,
                        mLocationListener);
                setCheckTimer(ComDef.LOCATION_CHECK_TIMER_DELAY,
                        ComDef.LOCATION_CHECK_TIMER_PERIOD);
            }
        }
    }

    private void onStop() {
        if (!mIsRunning) {
            LogUtils.td("already stopped");
        } else {
            if (TestData.isTestMode()) {
                TestData.stopTest();
            } else {
                removeCheckTimer();
                mLocationManager.removeUpdates(mLocationListener);
            }
            mTimeStop = System.currentTimeMillis();
            mIsRunning = false;
        }
    }

    public void onReset() {
        if (!mIsRunning) {
            resetRunningParameters();
        }
    }

    private void onCheckTimer() {
        Location location = mLocationManager.getLastKnownLocation(mLocationProvider);
        if (location == null) {
            LogUtils.td("last known location null");
        } else {
            LogUtils.td(new Location(location).toString() + ", current time = " + System.currentTimeMillis());
        }
    }

    private boolean isLocationChanged(Location l) {
        if (l == null) {
            return false;
        }
        Location lastLoc = getLastLocation();
        if (lastLoc == null) {
            return true;
        } else {
            return differentLocation(lastLoc, l);
        }
    }

    private boolean checkPermissions(Context context) {
        return (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }


    private static boolean isLocationValid(Location l) {
        return l.getAccuracy() < ComDef.VALID_ACCURACY_MAX;
    }

    public void onLocationChanged(Location location) {
        LogUtils.trace();
        if (!isLocationValid(location)) {
            LogUtils.ti("invalid location, accuracy = " + location.getAccuracy());
            return;
        }

        Location newLocation = new Location(location);
        mLocationList.add(newLocation);
        DataLogic.inst().onNewLocation(newLocation);

        if (mLocationMax == null || newLocation.getSpeed() > mLocationMax.getSpeed()) {
            mLocationMax = newLocation;
            DataLogic.inst().onNewMaxSpeed();
            MyApp.playSoundMaxSpeed();
        }

        if (!TestData.isTestMode()) {
            // save location info to log file
            LogEx.i(getLastLocationInfo());
        }

        /*
        for (LocationCallback callback : mCallbackList) {
            if (callback != null) {
                callback.onLocationUpdate();
            }
        }
        //*/
        if (mLocationCallback != null) {
            mLocationCallback.onLocationUpdate();
        }
    }

    private Location getLastLocation() {
        if (mLocationList.size() > 0) {
            return mLocationList.get(mLocationList.size() - 1);
        } else {
            return null;
        }
    }

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            LocationService.this.onLocationChanged(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            LogEx.trace();
        }

        @Override
        public void onProviderEnabled(String provider) {
            LogEx.trace();
        }

        @Override
        public void onProviderDisabled(String provider) {
            LogEx.trace();
        }
    };
}
