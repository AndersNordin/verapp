package chalmers.verapp;

import chalmers.verapp.base.BaseActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends BaseActivity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Create listener
		OnClickListener listener = new OnClickListener(){
			@Override
			public void onClick(View v){
				Intent intent = new Intent(MainActivity.this, RunActivity.class);
				startActivity(intent);		
			}			
		};

		// Identify start button
		Button btn = (Button) findViewById(R.id.start);
		// Connect listener and button
		btn.setOnClickListener(listener);
		
		// Exit correctly
		if( getIntent().getBooleanExtra("Exit me", false)){
	        finish();
	        return;
	    }
	}
}
