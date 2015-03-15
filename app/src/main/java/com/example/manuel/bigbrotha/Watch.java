package com.example.manuel.bigbrotha;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Tracking activity, watch class will take all the user preferences/settings and will start
 * sending the data based on the preferred frequency to the specified server
 * @designer Manuel Gonzales
 * @programmer Manuel Gonzales / Eric Tsang
 */
public class Watch extends Activity {


    private Connection connection;
    private LocationManager locationManager;
    private LocationListener locationListener;

    /**
     * Gets the data from the bundle passed by the Choose class, it will then try to connect to the server
     * send an ID (wifi MAC), then it will set up the criteria for the location and lastly will request
     * updates based on the frequency specified by the user. By using a location Listener.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);

        // parse starting intent
        Bundle net_parameters = getIntent().getExtras();
        String ipaddress = net_parameters.getString("ip");
        String options = net_parameters.getString("option");
        Integer port = net_parameters.getInt("port");
        Integer frequency = net_parameters.getInt("freq");
        Boolean data = net_parameters.getBoolean("data");
        Boolean wifi = net_parameters.getBoolean("wifi");

        // connect to server
        try {
            connection = new Connection(ipaddress, port);
            connection.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // send mac address to server
        try {
            String macaddress = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                    .getConnectionInfo().getMacAddress();
            connection.sendMessage("{\"id\":\"" + macaddress + "\"}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Log.d("TCP_MESSAGE", "Sent ID");

        // set up criteria object
        Criteria criteria = new Criteria();
        criteria.setSpeedRequired(true);
        criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setAltitudeRequired(true);
        criteria.setCostAllowed(data);

        Log.d("LOCATION", "Set Criteria");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, wifi);

        boolean time = options.equalsIgnoreCase("time");
        if (time) {
            frequency *= 1000;
        }

        Log.d("LOCATION", "Set Manager");

        /**
         * Class Location Listener, when there is a location change it will update the location
         * and will send it to the server in a formatted string.
         */
        locationListener = new LocationListener() {
            public void onLocationChanged(Location current_location) {
                Long tsLong = System.currentTimeMillis() / 1000;
                String ts = tsLong.toString();

                double latitude = current_location.getLatitude();
                double longitude = current_location.getLongitude();
                double altitude = current_location.getAltitude();
                double speed = current_location.getSpeed();

                String slatitude = Double.toString(latitude);
                String slongitude = Double.toString(longitude);
                String saltitude = Double.toString(altitude);
                String sspeed = Double.toString(speed);

                String sendloc = "{\"lat\":\"" + slatitude + "\", \"lon\":\"" + slongitude
                        + "\", \"speed\":\"" + sspeed + "\", \"altitude\":\"" + saltitude
                        + "\", \"timestamp\":\"" + ts + "\"}";

                Log.d("INFO", "Set Info");


                try {
                    connection.sendMessage(sendloc);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Log.d("TCP", "Sent Data");

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        Log.d("Listener", "Set Listener");
        Log.d("Listener", "time: " + time);
        Log.d("Listener", "frequency: " + frequency);
        if (time) {
            locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, false), frequency, 0, locationListener);
        } else {
            locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, false), 0, frequency, locationListener);
        }

        Log.d("STARTED", "Sending Locations");

        final ImageView animImageView = (ImageView) findViewById(R.id.ivAnimation);
        animImageView.setBackgroundResource(R.drawable.anim);
        animImageView.post(new Runnable() {
            @Override
            public void run() {
                AnimationDrawable frameAnimation =
                        (AnimationDrawable) animImageView.getBackground();
                frameAnimation.start();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.watch, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * In case the stop button is pressed it will stop the thread and will finish the activity.
     * @param view
     */
    public void stopWatch(View view) {
        connection.cancel();
        finish();
    }

    /**
     * The core thread of the activity, it will attempt connection to the server at first and after
     * succeeding it will go into a while loop that will wait for messages from the location listener
     * and will send these to the server.
     * On Error or after being canceled it will free the resources.
     */
    private class Connection extends Thread {
        public static final int MSG_TYPE_SEND = 0;
        public static final int MSG_TYPE_CANCEL = 1;

        private String ip_address;
        private Integer port_num;
        LinkedBlockingQueue<Message> msgq;

        public Connection(String ip_address, Integer port_num) {
            this.ip_address = ip_address;
            this.port_num = port_num;
            this.msgq = new LinkedBlockingQueue<Message>();
        }

        @Override
        public void run() {
            Socket socket = null;
            DataOutputStream os = null;
            boolean keepLooping;

            try {
                Log.d("TCP_MESSAGE", "Connecting...");
                try {
                    socket = new Socket(ip_address, port_num);
                    os = new DataOutputStream(socket.getOutputStream());
                } catch (UnknownHostException e) {
                    mHandler.post(mError);
                    finish();
                }


                keepLooping = true;

                while (keepLooping) {
                    Message msg = msgq.take();
                    Log.d("Connection", "looped: " + msg.what + ":" + msg.obj);
                    switch (msg.what) {
                        case MSG_TYPE_SEND:
                            try {
                                os.writeUTF(msg.obj.toString());
                            }
                            catch(IOException e)
                            {
                                mHandler.post(mError);
                                e.printStackTrace();
                            }
                            break;
                        case MSG_TYPE_CANCEL:
                            os.close();
                            socket.close();
                            keepLooping = false;
                            break;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void cancel() {
            Message msg = new Message();
            msg.what = MSG_TYPE_CANCEL;
            msgq.add(msg);
        }

        public void sendMessage(String message) {
            Message msg = new Message();
            msg.what = MSG_TYPE_SEND;
            msg.obj = message;
            msgq.add(msg);
        }
    }

    /**
     * Handler used to toast errors into the activity.
     */
    final Handler mHandler = new Handler();
    final Runnable mError = new Runnable() {
        public void run() {
            Toast.makeText(Watch.this, "Connection Error", Toast.LENGTH_SHORT).show();
        }

    };
}