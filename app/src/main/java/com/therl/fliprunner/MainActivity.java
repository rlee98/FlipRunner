package com.therl.fliprunner;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;



public class MainActivity extends AppCompatActivity {

    // first comment!
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button b1 = (Button) findViewById(R.id.button1);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                Intent startGame = new Intent(MainActivity.this, GameActivity.class);
                startActivity(startGame);
            }
        });
    }

}
