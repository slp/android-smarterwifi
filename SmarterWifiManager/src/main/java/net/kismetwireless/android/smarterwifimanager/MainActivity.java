package net.kismetwireless.android.smarterwifimanager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;


// Main icon color shifts
// 00e8d5    b8b8b8    a40000

public class MainActivity extends Activity {
    SmarterWifiServiceBinder serviceBinder;
    Context context;

    private SmarterWifiService.SmarterServiceCallback serviceCb = new SmarterWifiService.SmarterServiceCallback() {
        @Override
        public void handleLearningMode(final boolean learn) {
            super.handleLearningMode(learn);

            runOnUiThread(new Runnable() {
                public void run() {
                    ImageView lmi = (ImageView) findViewById(R.id.learningView);
                    TextView learntext = (TextView) findViewById(R.id.textLearning);

                    if (learn) {
                        lmi.setImageResource(R.drawable.custom_learning_enabled);
                        learntext.setText("Learning towers");
                    } else {
                        lmi.setImageResource(R.drawable.custom_learning_disabled);
                        learntext.setText("Inactive");
                    }
                }
            });
        }

        @Override
        public void handleTowerMode(final long towerid, final boolean active) {
            super.handleTowerMode(towerid, active);

            runOnUiThread(new Runnable() {
                public void run() {
                    ImageView ami = (ImageView) findViewById(R.id.towerView);
                    TextView towertext = (TextView) findViewById(R.id.textTowerId);
                    String idtext;

                    if (towerid < 0)
                        idtext = "";
                    else
                        idtext = ", " + Long.toString(towerid);

                    if (towerid < 0) {
                        ami.setImageResource(R.drawable.custom_tower_disabled);
                        towertext.setText("Cell disabled");
                    } else if (active) {
                        ami.setImageResource(R.drawable.custom_tower_enabled);
                        towertext.setText("Tracked" + idtext);
                    } else {
                        ami.setImageResource(R.drawable.custom_tower_inactive);
                        towertext.setText("Unknown" + idtext);
                    }
                }
            });
        }

        @Override
        public void handleWifiMode(final boolean wifi, final boolean network, final String ssid) {
            super.handleWifiMode(wifi, network, ssid);

            runOnUiThread(new Runnable() {
                public void run() {
                    TextView wifitext = (TextView) findViewById(R.id.textWifiSsid);
                    ImageView wifiimage = (ImageView) findViewById(R.id.wifiView);

                    if (wifi && !network) {
                        wifitext.setText("Enabled, disconnected");
                        wifiimage.setImageResource(R.drawable.custom_wifi_inactive);
                    } else if (wifi && network) {
                        wifitext.setText(ssid);
                        wifiimage.setImageResource(R.drawable.custom_wifi_enabled);
                    } else {
                        wifitext.setText("Disabled");
                        wifiimage.setImageResource(R.drawable.custom_wifi_disabled);
                    }

                }
            });
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        serviceBinder = new SmarterWifiServiceBinder(context);
        serviceBinder.addCallback(serviceCb);
        serviceBinder.doBindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
