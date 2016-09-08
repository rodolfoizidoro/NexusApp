package nexus.telepresenca;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/**
 * Created by Rocketz on 30/08/2016.
 */
public class ListaDispositivos extends ListActivity {

    private BluetoothAdapter bluetoothAdapter = null;

   public static String ENDERECO_MAC = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayAdapter<String> ArrayBluetooth = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> dispostivosParedados = bluetoothAdapter.getBondedDevices();

        if(dispostivosParedados.size() > 0){
            for(BluetoothDevice dispositivos : dispostivosParedados){
                String nomeBt = dispositivos.getName();
                String MacBt = dispositivos.getAddress();
                ArrayBluetooth.add(nomeBt +  " \n " + MacBt);
            }
            setListAdapter(ArrayBluetooth);

        }


    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String informacaoGeral = ((TextView ) v).getText().toString();

       // Toast.makeText(getApplicationContext(),"Info: " + informacaoGeral, Toast.LENGTH_LONG).show();

        String enderecoMac = informacaoGeral.substring(informacaoGeral.length() - 17);

        Intent retornaMac = new Intent();
        retornaMac.putExtra(ENDERECO_MAC, enderecoMac);
        setResult(RESULT_OK,retornaMac);
        finish();


    }
}
