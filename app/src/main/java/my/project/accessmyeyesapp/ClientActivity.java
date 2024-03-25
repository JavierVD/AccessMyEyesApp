package my.project.accessmyeyesapp;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import android.Manifest;

import my.project.accessmyeyesapp.ui.server_fragment_list.ServerFragmentList;

public class ClientActivity extends AppCompatActivity {
    public static int iconStatus = 0; //0 Send; 1 = Wait; 2 = Hang
    public String servidor = "", user = "";
    private String _id = "";
    private Socket socket;
    SendRequestThread clientThread = null;
    DeleteRequest deleteRequest = null;
    Thread threadHlander = null;
    public LottieAnimationView btnAccion;
    public TextView lblAccion;
    public Switch mic;
    private Streaming streaming;
    int[] anims = {R.raw.enviar_solicitud, R.raw.loading, R.raw.colgar};
    String[] labels = {"Comenzar", "Esperando", "Finalizar"};
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int MY_CAMERA_PERMISSION_CODE = 1001;
    private VideoClient videoClient;
    private Activity getActivity(){
        return this;
    }
    public CameraHandler mPreview;
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

        ImageButton btnHome = (ImageButton) findViewById(R.id.btnRegresarFromClient);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FirstActivity.class);
                startActivity(intent);
            }
        });


        btnAccion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(iconStatus == 0){
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

                }else
                if(iconStatus == 1){
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
                        if (threadHlander.isAlive()){
                            threadHlander.interrupt();
                        }


                    }
                }else
                if(iconStatus == 2){
                    iconStatus = 0;
                    JsonObject solicitudJson = new JsonObject();
                    solicitudJson.addProperty("action", "stop");
                    solicitudJson.addProperty("user", user);
                    solicitudJson.addProperty("id", _id);
                    new Thread(new StopCall(solicitudJson.toString())).start();

                    if (clientThread.isRunning()) {
                        clientThread.stop();
                        clientThread = null;
                    }
                    if (threadHlander.isAlive()){
                        threadHlander.interrupt();
                    }
                }
                Log.d("status icon: " , String.valueOf(iconStatus));
                Log.d("status label: " , labels[iconStatus]);
                btnAccion.setAnimation(anims[iconStatus]);
                btnAccion.playAnimation();
                lblAccion.setText(labels[iconStatus]);
            }
        });
    }
    private Context getActivityContext(){
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
            try{
                if(socket != null || socket.isConnected()){
                    Log.d("Arre", "arre");
                    socket.close();
                    socket = new Socket(servidor, 12321);
                    output = new PrintWriter(socket.getOutputStream(), true);
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                }else{
                    Log.d("No arre", ":C");
                }
                Log.d("Stoped request", _soliciturJson);
                output.println(_soliciturJson);
                output.flush();
            }catch (IOException ex){
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
            try{
                if(socket != null || socket.isConnected()){
                    Log.d("Arre", "arre");
                    socket.close();
                    socket = new Socket(servidor, 12321);
                    output = new PrintWriter(socket.getOutputStream(), true);
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                }else{
                    Log.d("No arre", ":C");
                }
                Log.d("Candeled request", _soliciturJson);
                output.println(_soliciturJson);
                output.flush();
            }catch (IOException ex){
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
                output = new PrintWriter(socket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                enviarSolicitud(solicitudJson);

                while (running) {

                    if(socket != null || socket.isConnected()){
                        String response = input.readLine();
                        if (response != null) {
                            Log.d("RECEIVED FROM SERVER:", response);
                            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                            String _id, _action;
                            _action = jsonResponse.get("action").getAsString();
                            if(_action.equals("cancel")){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        iconStatus = 0;
                                        LottieAnimationView btnAccion = (LottieAnimationView) findViewById(R.id.btnEnviarSolicitud);
                                        lblAccion = (TextView) findViewById(R.id.lblEjecucion);
                                        btnAccion.setAnimation(anims[iconStatus]);
                                        btnAccion.playAnimation();
                                        lblAccion.setText(labels[iconStatus]);
                                        if(socket != null && socket.isConnected()) {
                                            try {
                                                socket.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });
                                running = false;
                            }else
                                if(_action.equals("ok")){
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
                                            activarCamara();
                                            videoClient = new VideoClient(socket, getActivityContext());
                                            videoClient.ServerSocketThread();

                                        }
                                    });
                                }else if(_action.equals("close")){

                                    if(clientThread != null && clientThread.isRunning()){
                                        clientThread.stop();
                                    }
                                    if(socket != null && socket.isConnected()){
                                        socket.close();
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

    private void handleCamera(Socket socket, Context context){
        videoClient = new VideoClient(socket, context);
        videoClient.iniciarTransmision();
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
}