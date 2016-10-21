package nexus.telepresenca;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;



/**
 * Created by Rocketz on 21/10/2016.
 */

public class SplashScreen extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 3000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen);

        new Handler().postDelayed(new Runnable() {
            /*
             * Exibindo splash com um timer.
             */
            @Override
            public void run() {
                // Esse método será executado sempre que o timer acabar
                // E inicia a activity principal
                Intent i = new Intent(SplashScreen.this, Selecao.class);
                startActivity(i);

                // Fecha esta activity
                finish();
            }
        }, SPLASH_TIME_OUT);

    }
}
