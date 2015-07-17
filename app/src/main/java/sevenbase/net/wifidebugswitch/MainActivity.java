package sevenbase.net.wifidebugswitch;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (RootShell.isAccessGiven()) {
            if (RootShell.isRootAvailable()) {

            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("SU not available");
                builder.setMessage("Busybox is not available. Please install busybox before running this app");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        finish();
                    }
                });
                AlertDialog dialog = builder.show();

                TextView messageView = (TextView)dialog.findViewById(android.R.id.message);
                messageView.setGravity(Gravity.CENTER);
                TextView titleView = (TextView)dialog.findViewById(getResources().getIdentifier("alertTitle", "id", "android"));
                if (titleView != null) {
                    titleView.setGravity(Gravity.CENTER);
                }
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Couldn't gain root access");
            builder.setMessage("The app couldn't gain root access to your phone. Please root your device and give this app root permissions");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    finish();
                }
            });
            AlertDialog dialog = builder.show();

            TextView messageView = (TextView)dialog.findViewById(android.R.id.message);
            messageView.setGravity(Gravity.CENTER);
            TextView titleView = (TextView)dialog.findViewById(getResources().getIdentifier("alertTitle", "id", "android"));
            if (titleView != null) {
                titleView.setGravity(Gravity.CENTER);
            }
        }

        //Enable WiFi
        WifiManager wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        updateIP();

        final TextView conView = (TextView) findViewById(R.id.conView);

        //Check if ADB is running on port 5555
        final Command checkadb = new Command(0, "getprop | grep service.adb.tcp.port") {
            @Override
            public void commandOutput(int id, String line) {
                if (line.contains("[5555]")) {
                    //ADB over WiFi is enabled!
                    conView.setTextColor(getResources().getColor(R.color.green));
                    conView.setText("ADB over WiFi enabled");
                } else {
                    //ADB over WiFi is disabled!
                    conView.setTextColor(getResources().getColor(R.color.red));
                    conView.setText("ADB over WiFi disabled");
                }

                //MUST call the super method when overriding!
                super.commandOutput(id, line);
            }
        };

        //Start ADB on port 5555
        final Command startadb = new Command(0, "su", "setprop service.adb.tcp.port 5555", "stop adbd", "start adbd") {
            @Override
            public void commandCompleted(int id, int exitcode) {
                try {
                    RootShell.getShell(true).add(checkadb);
                } catch (IOException | TimeoutException | RootDeniedException e) {
                    e.printStackTrace();
                }
            }
        };

        //Stop ADB on port 5555
        final Command stopadb = new Command(0, "su", "setprop service.adb.tcp.port -1", "stop adbd", "start adbd") {
            @Override
            public void commandCompleted(int id, int exitcode) {
                try {
                    RootShell.getShell(true).add(checkadb);
                } catch (IOException | TimeoutException | RootDeniedException e) {
                    e.printStackTrace();
                }
            }
        };

        //Execute checkadb at startup
        try {
            RootShell.getShell(true).add(checkadb);
        } catch (IOException | RootDeniedException | TimeoutException e) {
            e.printStackTrace();
        }

        //Start adb when button is clicked
        final Button startbutton = (Button) findViewById(R.id.startbutton);
        startbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    RootShell.getShell(true).add(startadb);
                } catch (IOException | RootDeniedException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        });

        //Stop adb when button is clicked
        final Button stopbutton = (Button) findViewById(R.id.stopbutton);
        stopbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    RootShell.getShell(true).add(stopadb);
                } catch (IOException | RootDeniedException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        });

        //Update IP when ipView is clicked
        final TextView ipaddr = (TextView) findViewById(R.id.ipView);
        ipaddr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateIP();
            }
        });
    }

    private void updateIP() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        TextView ipView = (TextView) findViewById(R.id.ipView);
        ipView.setText(Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent aboutint = new Intent(getBaseContext(), AboutActivity.class);
            startActivity(aboutint);
        }

        return super.onOptionsItemSelected(item);
    }
}
