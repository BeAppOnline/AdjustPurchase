package online.beapp.apurchase;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import online.beapp.afpurchase.IAPActivity;

public class DefaultIAPActivity extends IAPActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_i_a_p);
    }
}
