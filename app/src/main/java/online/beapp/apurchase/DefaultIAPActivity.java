package online.beapp.apurchase;

import android.os.Bundle;
import online.beapp.afpurchase.IAPActivity;

public class DefaultIAPActivity extends IAPActivity {


    @Override
    public Class<?> setDefaultIAPActivity() {
        return this.getClass();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_i_a_p);
    }
}
