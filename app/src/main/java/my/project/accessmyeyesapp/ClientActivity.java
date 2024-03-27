package my.project.accessmyeyesapp;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xlythe.view.camera.CameraView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.Manifest;

public class ClientActivity extends AppCompatActivity {
    public static int iconStatus = 0; //0 Send; 1 = Wait; 2 = Hang
    public String servidor = "", user = "";
    private String _id = "";
    private Socket socket;
    SendRequestThread clientThread = null;
    Thread threadHlander = null;
    public TextView lblAccion;
    public Switch mic;
    int[] anims = {R.raw.enviar_solicitud, R.raw.loading, R.raw.colgar};
    String[] labels = {"Comenzar", "Esperando", "Finalizar"};
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int MY_CAMERA_PERMISSION_CODE = 1001;
    private VideoClient videoClient;

    private Activity getActivity() {
        return this;
    }

    private boolean isStreaming = false;
    private CameraManager cameraManager;
    private String cameraId;
    private CameraDevice _cameraDevice;
    private CameraCaptureSession _captureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Executor executor;
    private CameraView cameraView;
    private AudioTrack audioTrack;
    private static final int AUDIO_SAMPLE_RATE = 11025;
    private static final int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_BUFFER_SIZE = AudioRecord.getMinBufferSize(
            AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_CONFIG, AUDIO_FORMAT);

    private CallThread callThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        user = getIntent().getStringExtra("user");
        servidor = getIntent().getStringExtra("servidor");

        LottieAnimationView btnAccion = (LottieAnimationView) findViewById(R.id.btnEnviarSolicitud);
        lblAccion = (TextView) findViewById(R.id.lblEjecucion);
        mic = findViewById(R.id.switch1);
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        ImageButton btnHome = (ImageButton) findViewById(R.id.btnRegresarFromClient);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FirstActivity.class);
                startActivity(intent);
            }
        });
        executor = Executors.newSingleThreadExecutor();

        btnAccion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iconStatus == 0) {
                    iconStatus = 1;
                    _id = _id.isEmpty() ? Utils.generarIDUnico() : _id;
                    JsonObject solicitudJson = new JsonObject();
                    solicitudJson.addProperty("action", "add");
                    solicitudJson.addProperty("user", user);
                    solicitudJson.addProperty("id", _id);
                    if (clientThread == null || !clientThread.isRunning()) {
                        clientThread = new SendRequestThread(solicitudJson.toString());
                        threadHlander = new Thread(clientThread);
                        threadHlander.start();
                    }

                } else if (iconStatus == 1) {
                    Log.d("Second Click!", "YES");
                    iconStatus = 0;

                    if (socket != null || socket.isConnected()) {

                        JsonObject solicitudJson = new JsonObject();
                        solicitudJson.addProperty("action", "remove");
                        solicitudJson.addProperty("user", user);
                        solicitudJson.addProperty("id", _id);
                        new Thread(new DeleteRequest(solicitudJson.toString())).start();

                        if (clientThread.isRunning()) {
                            clientThread.stop();
                            clientThread = null;
                        }
                        if (threadHlander.isAlive()) {
                            threadHlander.interrupt();
                        }


                    }
                } else if (iconStatus == 2) {
                    iconStatus = 0;
                    JsonObject solicitudJson = new JsonObject();
                    solicitudJson.addProperty("action", "stop");
                    solicitudJson.addProperty("user", user);
                    solicitudJson.addProperty("id", _id);
                    new Thread(new StopCall(solicitudJson.toString())).start();

                    if (clientThread.isRunning()) {
                        clientThread.stop();
                        clientThread = null;
                        videoClient.stopStream();

                    }
                    if (threadHlander.isAlive()) {
                        threadHlander.interrupt();
                    }

                }
                Log.d("status icon: ", String.valueOf(iconStatus));
                Log.d("status label: ", labels[iconStatus]);
                btnAccion.setAnimation(anims[iconStatus]);
                btnAccion.playAnimation();
                lblAccion.setText(labels[iconStatus]);
            }
        });
    }

    private Context getActivityContext() {
        return this;
    }

    private ClientActivity getClientActivity(){
        return this;
    }

    private PrintWriter output;
    private BufferedReader input;

    class StopCall implements Runnable {
        private String _soliciturJson;

        StopCall(String solicitudJson) {
            this._soliciturJson = solicitudJson;
            Log.d("Stoped request", this._soliciturJson);
        }

        @Override
        public void run() {
            try {
                if (socket != null || socket.isConnected()) {
                    Log.d("Arre", "arre");
                    socket.close();
                    socket = new Socket(servidor, 12321);
                    output = new PrintWriter(socket.getOutputStream(), true);
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } else {
                    Log.d("No arre", ":C");
                }
                Log.d("Stoped request", _soliciturJson);
                output.println(_soliciturJson);
                output.flush();
            } catch (IOException ex) {
                Log.d("ERROR", ex.toString());
            }


            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    }

    class DeleteRequest implements Runnable {
        private String _soliciturJson;

        DeleteRequest(String solicitudJson) {
            this._soliciturJson = solicitudJson;
            Log.d("Candeled request", this._soliciturJson);
        }

        @Override
        public void run() {
            try {
                if (socket != null || socket.isConnected()) {
                    Log.d("Arre", "arre");
                    socket.close();
                    socket = new Socket(servidor, 12321);
                    output = new PrintWriter(socket.getOutputStream(), true);
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } else {
                    Log.d("No arre", ":C");
                }
                Log.d("Candeled request", _soliciturJson);
                output.println(_soliciturJson);
                output.flush();
            } catch (IOException ex) {
                Log.d("ERROR", ex.toString());
            }


            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });


        }
    }

    public class SendRequestThread implements Runnable {
        private String solicitudJson;
        private boolean running = true;
        private PrintWriter output;
        private BufferedReader input;

        public SendRequestThread(String solicitudJson) {
            this.solicitudJson = solicitudJson;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(servidor, 12321);
                if (!socket.isConnected()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "No se pudo conectar al servidor", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                output = new PrintWriter(socket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                enviarSolicitud(solicitudJson);

                while (running) {

                    if (socket != null || socket.isConnected()) {
                        String response = input.readLine();
                        if (response != null) {
                            Log.d("RECEIVED FROM SERVER:", response);
                            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                            String _id, _action;
                            _action = jsonResponse.get("action").getAsString();
                            if (_action.equals("cancel")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        iconStatus = 0;
                                        LottieAnimationView btnAccion = (LottieAnimationView) findViewById(R.id.btnEnviarSolicitud);
                                        lblAccion = (TextView) findViewById(R.id.lblEjecucion);
                                        btnAccion.setAnimation(anims[iconStatus]);
                                        btnAccion.playAnimation();
                                        lblAccion.setText(labels[iconStatus]);
                                        if (socket != null && socket.isConnected()) {
                                            try {
                                                socket.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });
                                running = false;
                            } else if (_action.equals("ok")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        iconStatus = 2;
                                        LottieAnimationView btnAccion = (LottieAnimationView) findViewById(R.id.btnEnviarSolicitud);
                                        activarMicrofono();
                                        lblAccion = (TextView) findViewById(R.id.lblEjecucion);
                                        btnAccion.setAnimation(anims[iconStatus]);
                                        btnAccion.playAnimation();
                                        lblAccion.setText(labels[iconStatus]);
                                        if (ActivityCompat.checkSelfPermission( getActivityContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getActivityContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ) {
                                            return;
                                        }
                                        cameraView = findViewById(com.xlythe.view.camera.R.id.camera);
                                        cameraView.open();;
                                        videoClient = new VideoClient(socket,cameraView,getClientActivity());
                                        videoClient.startStream();
                                        callThread = new CallThread(socket);
                                        callThread.execute();
                                        }
                                    });
                                }else if(_action.equals("close")){

                                    if(clientThread != null && clientThread.isRunning()){
                                        clientThread.stop();
                                    }
                                    if(socket != null && socket.isConnected()){
                                        socket.close();
                                    }
                                    if(callThread.isRunning){
                                        callThread.stopTask();
                                    }
                                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stop() {
            running = false;
        }

        public boolean isRunning() {
            return running;
        }

        public void enviarSolicitud(String solicitud) {
            Log.d("SOLICITUD:", solicitud);
            output.println(solicitud);
            output.flush();
        }
    }

    class CallThread extends AsyncTask<Void, String, Void> {

        private Socket clientSocket;
        private String _id;
        private boolean isRunning = false;
        public CallThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void stopTask(){
            try {
                cancel(true);
                isRunning = false;
                if(clientSocket.isConnected())
                    clientSocket.close();
                audioTrack.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getId(){
            return this._id;
        }
        public Socket getSocket() {return this.clientSocket;}

        @Override
        protected Void doInBackground(Void... params) {
            try (InputStream inputStream = clientSocket.getInputStream()) {

                DataInputStream is = new DataInputStream(inputStream);
                isRunning = true;
                while (isRunning) {
                    try {
                        int token = is.readInt();
                        if (token == 4) {
                            String header = is.readUTF();
                            if (header.equals("*@@*")) {
                                int audioLength = is.readInt();
                                is.readUTF();
                                byte[] buffers = new byte[audioLength];
                                int len = 0;
                                while (len < audioLength) {
                                    len += is.read(buffers, len, audioLength - len);
                                }
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(audioTrack != null){
                                            audioTrack.write(buffers, 0, buffers.length);
                                            audioTrack.play();
                                        }

                                        else
                                            Log.e("AUDIO TRACK NOT FOUND","LOSED!");
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

        }
    }

    private void activarMic() {
        mic.setVisibility(View.VISIBLE);
        mic.setChecked(true);
    }

    private void activarMicrofono() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                PERMISSION_REQUEST_CODE);
    }

    private void activarCamara(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                MY_CAMERA_PERMISSION_CODE);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                activarMic();
            } else {
                Toast.makeText(this, "El permiso para usar el micrófono fue denegado",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            _cameraDevice = cameraDevice;
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            _cameraDevice.close();
        }
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            _cameraDevice.close();
            _cameraDevice = null;

        }
    };

}