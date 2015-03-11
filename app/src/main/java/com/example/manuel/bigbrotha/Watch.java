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
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Timestamp;

/**
 * @author  MG
 */
public class Watch extends Activity {


    private startConnection new_connection;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);


        Bundle net_parameters = getIntent().getExtras();
        String ipaddress = net_parameters.getString("ip");
        String option = net_parameters.getString("option");
        Integer port = net_parameters.getInt("port");
        Integer frequency = net_parameters.getInt("freq");
        Boolean data = net_parameters.getBoolean("data");
        Boolean wifi = net_parameters.getBoolean("wifi");

        new_connection = new startConnection(ipaddress, port, frequency, data, wifi, option);
        new_connection.execute();

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

    public void stopWatch(View view)
    {
        new_connection.cancel(true);
        finish();
    }

    class startConnection extends AsyncTask<Integer, Integer, Integer>
    {
        String ip_address;
        String options;
        Integer port_num;
        Integer frequency;
        Socket socket;
        DataOutputStream os;
        Boolean data;
        Boolean wifi;
        Boolean time;
        Boolean started;
        LocationManager locationManager;
        LocationListener locationListener;

        public startConnection(String ip_address, Integer port_num, Integer frequency, Boolean data, Boolean wifi, String options)
        {
            this.ip_address = ip_address;
            this.port_num = port_num;
            this.frequency = frequency;
            this.data = data;
            this.wifi = wifi;
            this.options = options;
            started = false;
        }

        @Override
        protected Integer doInBackground(Integer... strings)
        {
            try {

                Log.d("TCP_MESSAGE", "Connecting...");

                Looper.prepare();

                socket = new Socket(ip_address, port_num);

                WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = manager.getConnectionInfo();
                String address = info.getMacAddress();
                String maddress = "{\"id\":\"" + address +"\"}";

                started = true;
                Log.d("WIFI", "Retrieved MAC");

                os = new DataOutputStream(socket.getOutputStream());
                os.writeUTF(maddress);

                Log.d("TCP_MESSAGE", "Sent ID");

                Criteria criteria = new Criteria();
                criteria.setSpeedRequired(true);
                criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);
                criteria.setAltitudeRequired(true);

                criteria.setCostAllowed(data);

                Log.d("LOCATION", "Set Criteria");

                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                //locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, wifi);

                if(options.equalsIgnoreCase("time"))
                {
                    time = true;
                    frequency = frequency * 1000;
                }
                else
                {
                    time = false;
                }


                Log.d("LOCATION", "Set Manager");

                locationListener = new LocationListener()
                {

                    double latitude = 0;
                    double longitude = 0;
                    double altitude = 0;
                    double speed = 0;
                    String slatitude;
                    String slongitude;
                    String saltitude;
                    String sspeed;
                    String sendloc;

                    public void onLocationChanged(Location current_location)
                    {
                        Long tsLong = System.currentTimeMillis()/1000;
                        String ts = tsLong.toString();

                        latitude = current_location.getLatitude();
                        longitude = current_location.getLongitude();
                        altitude = current_location.getAltitude();
                        speed = current_location.getSpeed();

                        slatitude = Double.toString(latitude);
                        slongitude = Double.toString(longitude);
                        saltitude = Double.toString(altitude);
                        sspeed = Double.toString(speed);

                        sendloc = "{\"lat\":\"" + slatitude + "\", \"lon\":\"" + slongitude + "\", \"speed\":\"" + sspeed +
                                "\", \"altitude\":\"" + saltitude + "\", \"timestamp\":\"" + ts + "\"}";


                        Log.d("INFO", "Set Info");

                        try
                        {
                            os.writeUTF(sendloc);
                        }
                        catch(Exception e)
                        {
                            throw new RuntimeException(e);
                        }

                        Log.d("TCP", "Sent Data");

                    }

                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    public void onProviderEnabled(String provider) {}

                    public void onProviderDisabled(String provider) {}

                };


                Log.d("Listener", "Set Listener");

                Log.d("Listener", "time: "+time);
                Log.d("Listener", "frequency: "+frequency);
                if(time)
                {
                    locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, false), frequency, 0, locationListener);
                }
                else
                {
                    locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, false), 0, frequency, locationListener);
                }


                Log.d("STARTED", "Sending Locations");

                return null;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }


        /*protected void onProgressUpdate(Integer value) {
            if(value == 1)
                Toast.makeText(Watch.this, "STEP 1", Toast.LENGTH_SHORT).show();
            if(value == 2)
                Toast.makeText(Watch.this, "STEP 2", Toast.LENGTH_SHORT).show();
            if(value == 3)
                Toast.makeText(Watch.this, "STEP 3", Toast.LENGTH_SHORT).show();
            if(value == 4)
                Toast.makeText(Watch.this, "STEP 4", Toast.LENGTH_SHORT).show();

        }

        protected void onPostExecute()
        {
            try {
                os.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        @Override
        protected void onCancelled()
        {
            try {
                if (started) {
                    os.close();
                    socket.close();
                    locationManager.removeUpdates(locationListener);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
