package nexus.telepresenca;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter = null;
    BluetoothDevice bluetoothDevice = null;
    BluetoothSocket bluetoothSocket = null;

    private static final int SOLICITA_ATIVACAO = 1;
    private static final int SOLICITA_CONEXAO = 2;

    private Button btnConexao;
    private Button btnFrente;
    private Button btnVoltar;
    private Button btnEsquerda;
    private Button btnDireita;
    private Button btnStop;
    private Button btnServidor;
    private ConnectedThread mConnectedThread;
    private boolean conexao = false;
    private static String MAC = null;
    private UUID  uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private SeekBar seekBar;
    private String URL = "http://www.meucontatoapp.com/nexus/api/select.php";
    ServiceStatus mService;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConexao = (Button) findViewById(R.id.btnConectar);
        btnFrente = (Button) findViewById(R.id.btnFrente);
        btnVoltar = (Button) findViewById(R.id.btnVoltar);
        btnDireita = (Button) findViewById(R.id.btnDireita);
        btnEsquerda = (Button) findViewById(R.id.btnEsquerda);
        btnStop = (Button) findViewById(R.id.btnStop);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        btnServidor = (Button) findViewById(R.id.btnServidor);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null){
            Toast.makeText(getApplicationContext(),"Seu dispositivo não possui bluetooth.", Toast.LENGTH_LONG).show();
            //new SurveyDBAsyncTask().execute("");

        }
        else if(!bluetoothAdapter.isEnabled() ){
            Intent  intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, SOLICITA_ATIVACAO);

        }

        btnServidor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Toast.", Toast.LENGTH_LONG).show();
            }
        });

        btnConexao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(conexao){
                    //desconectar
                    try{
                        conexao = false;
                        bluetoothSocket.close();
                        Toast.makeText(getApplicationContext(),"Bleutooth desconectado.", Toast.LENGTH_LONG).show();
                        btnConexao.setText("Conectar");
                        mConnectedThread.currentThread().stop();
                    }
                    catch (IOException error){
                        Toast.makeText(getApplicationContext(),"Erro ao desconetar : " + error, Toast.LENGTH_LONG).show();
                    }

                }
                else {
                    //conectar

                    Intent abreLista = new Intent(MainActivity.this,ListaDispositivos.class);
                    startActivityForResult(abreLista,SOLICITA_CONEXAO);
                }

            }
        });

        btnFrente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write("f");
            }
        });

        btnVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write("b");
            }
        });

        btnDireita.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write("r");
            }
        });

        btnEsquerda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write("l");
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write("s");
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case SOLICITA_ATIVACAO:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(getApplicationContext(),"O bluetooth foi ativado.", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getApplicationContext(),"O bluetooth não foi ativado.", Toast.LENGTH_LONG).show();
                    //finish();
                }
                break;
            case SOLICITA_CONEXAO:
                if(resultCode == Activity.RESULT_OK){

                    MAC = data.getExtras().getString(ListaDispositivos.ENDERECO_MAC);

                    bluetoothDevice =  bluetoothAdapter.getRemoteDevice(MAC);
                    byte[] pin;
                    pin = ("1234").getBytes();
            //        bluetoothDevice.setPin(pin);
///
                    try{
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);

                        bluetoothSocket.connect();
                        Toast.makeText(getApplicationContext(),"Conectado com MAC: " + MAC, Toast.LENGTH_LONG).show();
                        conexao = true;
                        btnConexao.setText("Desconectar");
                        mConnectedThread = new ConnectedThread(bluetoothSocket);
                        mConnectedThread.start();
                        new SurveyDBAsyncTask().execute("");
                    //    Intent intent = new Intent(this, ServiceStatus.class);
                      //  startService(intent);
                    }
                    catch (IOException error){
                        conexao = false;
                        Toast.makeText(getApplicationContext(),"Erro ao conectar : " + error, Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(),"Falha ao obter o MAC.", Toast.LENGTH_LONG).show();
                    //finish();
                }
                break;
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                String velocidade = String.valueOf(progress);

                Toast.makeText(getApplicationContext(),"Velocidade: " + velocidade,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

//            // Keep looping to listen for received messages
//            while (true) {
//                try {
//                    bytes = mmInStream.read(buffer);        	//read bytes from input buffer
//                    String readMessage = new String(buffer, 0, bytes);
//                    // Send the obtained bytes to the UI Activity via handler
//                   // bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
//                } catch (IOException e) {
//                    break;
//                }
//            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }

    }

    //Create a new AsyncTask
    private class SurveyDBAsyncTask extends AsyncTask<String, Void, Long>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p/>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        private TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        final RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

                        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                    mConnectedThread.write(response);
                                // Toast.makeText(getApplicationContext(),response,Toast.LENGTH_LONG).show();
                                requestQueue.stop();

                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });
                        requestQueue.add(stringRequest);
                    }
                });


            }
        };


        @Override
        protected Long doInBackground(String... params) {
            long id = 0;
            try {

               Timer timer = new Timer();
                timer.schedule(timerTask,2000,1000);

            }catch (SQLiteException e){
            }
            return id;
        }

        @Override
        protected void onPostExecute(Long id) {
            super.onPostExecute(id);

        }
    }

}
