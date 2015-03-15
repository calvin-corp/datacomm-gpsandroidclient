package com.example.manuel.bigbrotha;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

/**
 * Settings activity, choose class has the auto-generated methods plus two extra methods to clear the fields
 *  or move into the next activity and pass all the user preferences/settings.
 * @author  Manuel Gonzales
 */
public class Choose extends Activity {


    //globals to parse the data from the text fields/checkboxes/radio buttons
    EditText iptext;
    EditText porttext;
    EditText freqtext;
    RadioButton timebutton;
    RadioButton distancebutton;
    CheckBox data;
    CheckBox wifi;

    /**
     * Will get the data from the respective fields by ID
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);

        iptext = (EditText)findViewById(R.id.ipaddress);
        porttext = (EditText)findViewById(R.id.portnumber);
        freqtext = (EditText)findViewById(R.id.reqfreq);

        timebutton = (RadioButton)findViewById(R.id.time);
        distancebutton = (RadioButton)findViewById(R.id.distance);

        data = (CheckBox)findViewById(R.id.dcharges);
        wifi = (CheckBox)findViewById(R.id.dwifi);


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.choose, menu);
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
     * Clears all the text fields in the settings screen
     * @param view
     */
    public void clearFields(View view)
    {
        iptext.getText().clear();
        porttext.getText().clear();
        freqtext.getText().clear();
    }

    /**
     * Will get all the values entered by the user and will then validate each of them,
     * if all of them are valid it will pass them in an intent/bundle to the next activity
     * if one of them is invalid it will prompt the user and will return.
     * @param view
     */
    public void startWatch(View view)
    {
       String ip = iptext.getText().toString();
       String port = porttext.getText().toString();
       String frequency = freqtext.getText().toString();
       String option = "";
       int portnum;
       int freqint = 0;
       boolean datas = false;
       boolean wifis = false;

        if (ip.matches("")) {
            Toast.makeText(this, "You did not enter an Ip Address", Toast.LENGTH_SHORT).show();
            return;
        }
        else
        {
            if(!ValidateIP.isValidIPV4(ip))
            {
                Toast.makeText(this, "The IP Address is not valid", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (port.matches("")) {
            Toast.makeText(this, "You did not enter a Port Number", Toast.LENGTH_SHORT).show();
            return;
        }
        else
        {
            try {
                portnum = Integer.parseInt(port);
            }
            catch (NumberFormatException e) {
                Toast.makeText(this, "The Port Number is not valid", Toast.LENGTH_SHORT).show();
                return;
            }
        }

         if (frequency.matches(""))
         {
                Toast.makeText(this, "You did not enter a frequency interval", Toast.LENGTH_SHORT).show();
                return;
         }
         else
         {
                try {
                    freqint = Integer.parseInt(frequency);
                }
                catch (NumberFormatException e) {
                    Toast.makeText(this, "The time entered is not valid", Toast.LENGTH_SHORT).show();
                    return;
                }

                option = "time";
         }


        if(distancebutton.isChecked())
        {
            option = "distance";
        }
        else
        {
            option = "time";
        }

        if(data.isChecked())
        {
            datas = true;
        }

        if(wifi.isChecked())
        {
            wifis = true;
        }

        Intent start = new Intent(Choose.this, Watch.class);
        start.putExtra("ip", ip);
        start.putExtra("port", portnum);
        start.putExtra("option", option);
        start.putExtra("freq", freqint);
        start.putExtra("data", datas);
        start.putExtra("wifi", wifis);

        startActivity(start);
    }
}
