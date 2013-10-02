package net.kismetwireless.android.smarterwifimanager;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;

public class TimeRangeActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timerange);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.timerange, menu);
        return true;
    }
    
}
