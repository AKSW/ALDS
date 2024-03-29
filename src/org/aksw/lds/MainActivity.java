package org.aksw.lds;

import org.aksw.lds.service.BackgroundService;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        final Button startButton = (Button) findViewById(R.id.button1);
    	startButton.setText("Start service");
    	startButton.setTag(0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    
    public void buttonClick(View view)
    {
    	Log.i("Button", "Button pressed");
    	// get button
    	final Button startButton = (Button) findViewById(R.id.button1);
    	
    	final int status = (Integer)startButton.getTag();
    	if(status == 0) {
    		// start bg server
            this.startService(new Intent(MainActivity.this, BackgroundService.class));
    	    // change text
        	startButton.setText("Stop service");
        	startButton.setTag(1);
    	} else {
    		// start bg server
            this.stopService(new Intent(MainActivity.this, BackgroundService.class));
    		// change text
        	startButton.setText("Start service");
        	startButton.setTag(0);
    	} 
    }
}
