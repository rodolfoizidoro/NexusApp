package nexus.telepresenca;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

/**
 * Created by Rocketz on 21/10/2016.
 */

public class Selecao extends AppCompatActivity  {

    private Button btnconfirmar;
    private RadioButton rdbcliente;
    private RadioButton rdbcontroler;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selecao);

        btnconfirmar = (Button) findViewById(R.id.btnconfirmar);
        rdbcliente = (RadioButton) findViewById(R.id.cliente);
        rdbcontroler = (RadioButton) findViewById(R.id.controler);




        btnconfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(rdbcliente.isChecked() || rdbcontroler.isChecked())
                {
                    String selecao;

                    if(rdbcliente.isChecked())
                        selecao = "cliente";
                    else
                    selecao = "controlador";


                    Intent i = new Intent(Selecao.this, MainActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("selecao", selecao);
                    i.putExtras(bundle);
                    startActivity(i);

                }
                else{
                    Toast.makeText(Selecao.this, "Selecionar uma opção Cliente ou Controlador", Toast.LENGTH_SHORT).show();

                }

            }
        });


    }
}
