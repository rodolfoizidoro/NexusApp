package nexus.telepresenca;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import nexus.telepresenca.Util.PermissionUtils;
import nexus.telepresenca.Video.HelloWorldActivity;
import nexus.telepresenca.Video.config.OpenTokConfig;
import nexus.telepresenca.Video.services.ClearNotificationService;

public class MainActivity extends AppCompatActivity implements
        Session.SessionListener, Publisher.PublisherListener,
        Subscriber.VideoListener  {

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
  //  private SeekBar seekBar;
    private String URL = "http://www.meucontatoapp.com/nexus/api/select.php";
    ServiceStatus mService;
    boolean mBound = false;
    private PermissionUtils perm;
    private static final String LOGTAG = "demo-hello-world";
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    private ArrayList<Stream> mStreams;
    private Handler mHandler = new Handler();

    private RelativeLayout mPublisherViewContainer;
    private RelativeLayout mSubscriberViewContainer;
    private SurveyDBAsyncTask surveyDBAsyncTask;

    // Spinning wheel for loading subscriber view
    private ProgressBar mLoadingSub;

    private boolean resumeHasRun = false;

    private boolean mIsBound = false;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager mNotificationManager;
    private ServiceConnection mConnection;
    private Button btnCamera;
    private Timer timer;
    private Button btnFinalizarVideo;

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
        btnServidor = (Button) findViewById(R.id.btnServidor);
        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnFinalizarVideo = (Button) findViewById(R.id.btnFinalizarVideo);

        String[] permissoes = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        perm = new PermissionUtils();
        perm.validate(this,0,permissoes);


        mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
        mSubscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);
        mLoadingSub = (ProgressBar) findViewById(R.id.loadingSpinner);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mStreams = new ArrayList<Stream>();

      //  sessionConnect();

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
                //Toast.makeText(getApplicationContext(),"Toast.", Toast.LENGTH_LONG).show();
//                Intent intent = new Intent(MainActivity.this, HelloWorldActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent);
//                surveyDBAsyncTask=   new SurveyDBAsyncTask();
//                surveyDBAsyncTask.execute("");
                sessionConnect();
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

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSwapCamera();
            }
        });
        btnFinalizarVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                surveyDBAsyncTask.cancel(true);
                timer.cancel();
                timer.purge();

                if (mSession != null) {
                    mSession.disconnect();
                }
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
    public void onPause() {
        super.onPause();

        if (mSession != null) {
            mSession.onPause();

            if (mSubscriber != null) {
                mSubscriberViewContainer.removeView(mSubscriber.getView());
            }
        }

        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(this.getTitle())
                .setContentText(getResources().getString(R.string.notification))
                .setSmallIcon(R.mipmap.ic_launcher).setOngoing(true);

        Intent notificationIntent = new Intent(this, HelloWorldActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        mNotifyBuilder.setContentIntent(intent);
        if (mConnection == null) {
            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    ((ClearNotificationService.ClearBinder) binder).service.startService(new Intent(MainActivity.this, ClearNotificationService.class));
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotificationManager.notify(ClearNotificationService.NOTIFICATION_ID, mNotifyBuilder.build());
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    mConnection = null;
                }

            };
        }

        if (!mIsBound) {
            bindService(new Intent(MainActivity.this,
                            ClearNotificationService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
            mIsBound = true;
            startService(notificationIntent);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        if (!resumeHasRun) {
            resumeHasRun = true;
            return;
        } else {
            if (mSession != null) {
                mSession.onResume();
            }
        }
        mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);

        reloadInterface();
    }

    public void onSwapCamera() {
        if (mPublisher != null) {
            mPublisher.swapCamera();
        }
    }
    @Override
    public void onStop() {
        super.onStop();

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        if (isFinishing()) {
            mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
            if (mSession != null) {
                mSession.disconnect();
            }
        }
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        if (mSession != null) {
            mSession.disconnect();
        }

        super.onDestroy();
        finish();
    }
    private void sessionConnect() {
        if (mSession == null) {
            mSession = new Session(MainActivity.this,
                    OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);
            mSession.setSessionListener(this);
            mSession.connect(OpenTokConfig.TOKEN);
        }
    }

    @Override
    public void onBackPressed() {
        if (mSession != null) {
            mSession.disconnect();
        }

        super.onBackPressed();
    }

    public void reloadInterface() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSubscriber != null) {
                    attachSubscriberView(mSubscriber);
                }
            }
        }, 500);
    }

    @Override
    public void onConnected(Session session) {
        Log.i(LOGTAG, "Connected to the session.");
        if (mPublisher == null) {
            mPublisher = new Publisher(MainActivity.this, "publisher");
            mPublisher.setPublisherListener(this);
            attachPublisherView(mPublisher);
            mSession.publish(mPublisher);
        }
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOGTAG, "Disconnected from the session.");
        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getView());
        }

        if (mSubscriber != null) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
        }

        mPublisher = null;
        mSubscriber = null;
        mStreams.clear();
        mSession = null;
    }

    private void subscribeToStream(Stream stream) {
        mSubscriber = new Subscriber(MainActivity.this, stream);
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);

        if (mSubscriber.getSubscribeToVideo()) {
            // start loading spinning
            mLoadingSub.setVisibility(View.VISIBLE);
        }
    }

    private void unsubscribeFromStream(Stream stream) {
        mStreams.remove(stream);
        if (mSubscriber.getStream().equals(stream)) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSubscriber = null;
            if (!mStreams.isEmpty()) {
                subscribeToStream(mStreams.get(0));
            }
        }
    }

    private void attachSubscriberView(Subscriber subscriber) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                .getDisplayMetrics().heightPixels);
        mSubscriberViewContainer.removeView(mSubscriber.getView());
        mSubscriberViewContainer.addView(mSubscriber.getView(), layoutParams);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
    }

    private void attachPublisherView(Publisher publisher) {
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                320, 240);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
                RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                RelativeLayout.TRUE);
        layoutParams.bottomMargin = dpToPx(8);
        layoutParams.rightMargin = dpToPx(8);
        mPublisherViewContainer.addView(mPublisher.getView(), layoutParams);
    }

    @Override
    public void onError(Session session, OpentokError exception) {
        Log.i(LOGTAG, "Session exception: " + exception.getMessage());
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {

        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            mStreams.add(stream);
            if (mSubscriber == null) {
                subscribeToStream(stream);
            }
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            if (mSubscriber != null) {
                unsubscribeFromStream(stream);
            }
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisher, Stream stream) {
        if (OpenTokConfig.SUBSCRIBE_TO_SELF) {
            mStreams.add(stream);
            if (mSubscriber == null) {
                subscribeToStream(stream);
            }
        }
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisher, Stream stream) {
        if ((OpenTokConfig.SUBSCRIBE_TO_SELF && mSubscriber != null)) {
            unsubscribeFromStream(stream);
        }
    }

    @Override
    public void onError(PublisherKit publisher, OpentokError exception) {
        Log.i(LOGTAG, "Publisher exception: " + exception.getMessage());
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriber) {
        Log.i(LOGTAG, "First frame received");

        // stop loading spinning
        mLoadingSub.setVisibility(View.GONE);
        attachSubscriberView(mSubscriber);
    }

    /**
     * Converts dp to real pixels, according to the screen density.
     *
     * @param dp A number of density-independent pixels.
     * @return The equivalent number of real pixels.
     */
    private int dpToPx(int dp) {
        double screenDensity = this.getResources().getDisplayMetrics().density;
        return (int) (screenDensity * (double) dp);
    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriber, String reason) {
        Log.i(LOGTAG,
                "Video disabled:" + reason);
    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriber, String reason) {
        Log.i(LOGTAG, "Video enabled:" + reason);
    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriber) {
        Log.i(LOGTAG, "Video may be disabled soon due to network quality degradation. Add UI handling here.");
    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriber) {
        Log.i(LOGTAG, "Video may no longer be disabled as stream quality improved. Add UI handling here.");
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
                        surveyDBAsyncTask=   new SurveyDBAsyncTask();
                        surveyDBAsyncTask.execute("");
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
                                  // Log.i("Comandos",response);
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
                timer = new Timer();
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
