package net.kismetwireless.android.smarterwifimanager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;

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

                    if (learn) {
                        lmi.setImageResource(R.drawable.custom_learning_enabled);
                    } else {
                        lmi.setImageResource(R.drawable.custom_learning_disabled);
                    }
                }
            });
        }

        @Override
        public void handleActiveMode(final boolean active) {
            super.handleActiveMode(active);

            runOnUiThread(new Runnable() {
                public void run() {
                    ImageView ami = (ImageView) findViewById(R.id.towerView);

                    if (active) {
                        ami.setImageResource(R.drawable.custom_tower_enabled);
                    } else {
                        ami.setImageResource(R.drawable.custom_tower_disabled);
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
