package online.beapp.apurchase;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import online.beapp.afpurchase.IAPActivity;

public class MainActivity extends IAPActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DefaultIAPActivity = DefaultIAPActivity.class;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }
}
