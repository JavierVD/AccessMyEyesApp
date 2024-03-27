package my.project.accessmyeyesapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.Image;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import my.project.accessmyeyesapp.databinding.ServerActivityMainBinding;
import my.project.accessmyeyesapp.ui.server_fragment_list.AlertaCard;
import my.project.accessmyeyesapp.ui.server_fragment_list.ServerFragmentList;
import my.project.accessmyeyesapp.ui.server_fragment_main.ServerFragmentMain;


public class ServerActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ServerActivityMainBinding binding;
    public static final int SERVER_PORT = 12321;
    public static String SERVER_IP = "";
    public ServerFragmentList serverFragmentList;
    private List<ServerThread> serverThreads = new ArrayList<>();
    public List<AlertaCard> listaAlertas;
    public NavController navController;
    ServerSocket serverSocket;
    Thread mainSocketThread;
    AudioServer audioServer;
    public ConstraintLayout layout;
    private Boolean isRunning = true;
    private AudioTrack audioTrack;
    private static final int AUDIO_SAMPLE_RATE = 11025;
    private static final int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_BUFFER_SIZE = AudioRecord.getMinBufferSize(
            AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_CONFIG, AUDIO_FORMAT);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ServerActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_server_main, R.id.nav_lista_alertas, R.id.nav_cerrar)
                .setOpenableLayout(drawer)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        layout = findViewById(R.id.layoutFree);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_CONFIG,
                AUDIO_FORMAT, AUDIO_BUFFER_SIZE, AudioTrack.MODE_STREAM);


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
                if (item.getItemId() == R.id.nav_cerrar) {
                    handleDestroy();

                    finish();
                } else {
                    NavigationUI.onNavDestinationSelected(item, navController, false);
                    drawer.closeDrawers();
                }
                return false;
            }
        });
        //SOCKET


        FragmentManager fragmentManager = getSupportFragmentManager();
        serverFragmentList = (ServerFragmentList) fragmentManager.findFragmentById(R.id.nav_server_main);
        listaAlertas = ServerFragmentList.listaAlertas;

        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("IP", SERVER_IP);
                            TextView lblIP = findViewById(R.id.lblIP);
                            lblIP.setText("Dirección IP: " + SERVER_IP);
                        }
                    });
                    while (isRunning) {
                        Socket clientSocket = null;
                        try {
                            clientSocket = serverSocket.accept();

                            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            String jsonString = input.readLine();
                            if(jsonString != null){

                                JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
                                String action = jsonObject.get("action").getAsString();
                                Log.d("RECEIVED ACTION TO SERVER",action);
                                if (action.equals("add")) {
                                    String id = jsonObject.get("id").getAsString();
                                    boolean idExists = false;
                                    for (ServerThread thread : serverThreads) {
                                        if (thread.getId().equals(id)) {
                                            idExists = true;
                                            break;
                                        }
                                    }
                                    if (!idExists) {
                                        ServerThread serverThread = new ServerThread(clientSocket, id);
                                        serverThreads.add(serverThread);

                                        Log.d("ACTIVE THREADS: ", String.valueOf(serverThreads.size()));
                                        Log.d("ID!!!!", serverThread.getId());
                                        String nombre = jsonObject.get("user").getAsString();
                                        listaAlertas.add(new AlertaCard(nombre,id));
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ServerFragmentList.refreshAlertList();
                                            }
                                        });
                                        serverThread.execute();
                                    }
                                } else
                                    if(action.equals("remove")){
                                        String idToRemove = jsonObject.get("id").getAsString();

                                        ServerThread removedServerThread = null;
                                        Iterator<ServerThread> iterator = serverThreads.iterator();
                                        while (iterator.hasNext()) {
                                            ServerThread serverThread = iterator.next();
                                            if (serverThread.getId().equals(idToRemove)) {
                                                serverThread.cancel(true);
                                                removedServerThread = serverThread;
                                                iterator.remove();
                                                Log.d("REMOVED", "Process with ID " + idToRemove + " removed from the list");
                                                break;
                                            }
                                        }

                                        if (removedServerThread != null) {
                                            Iterator<AlertaCard> listaAlertasIterator = listaAlertas.iterator();
                                            while (listaAlertasIterator.hasNext()) {
                                                AlertaCard alerta = listaAlertasIterator.next();
                                                if (alerta.getId().equals(idToRemove)) {
                                                    listaAlertasIterator.remove();
                                                    Log.d("REMOVED", "Alert with ID " + idToRemove + " removed from the list");
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            ServerFragmentList.refreshAlertList();
                                                        }
                                                    });
                                                    break;
                                                }
                                            }
                                        }

                                    }else
                                    if(action.equals("stop")){
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ServerFragmentMain.clear();

                                            }
                                        });
                                        audioServer.stopStream();
                                        String idToRemove = jsonObject.get("id").getAsString();
                                        Iterator<ServerThread> iterator = serverThreads.iterator();
                                        while (iterator.hasNext()) {
                                            ServerThread serverThread = iterator.next();
                                            if (serverThread.getId().equals(idToRemove)) {
                                                serverThread.stopTask();
                                                audioServer.stopStream();
                                                iterator.remove();
                                                Log.d("REMOVED", "Process with ID " + idToRemove + " removed from the list");
                                                break;
                                            }
                                        }
                                    }else{
                                        Log.d("NOT ADDED", "NOT");
                                    }
                                }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            SERVER_IP = getLocalIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private ServerActivity getServerActivity(){
        return this;
    }

    private PrintWriter output;
    private BufferedReader input;

    class ServerThread extends AsyncTask<Void, String, Void> {

        private Socket clientSocket;
        private String _id;
        private ImageView videoPreview;
        private boolean isRunning = false;
        public ServerThread(Socket clientSocket, String id) {
            this.clientSocket = clientSocket;
            this._id = id;
        }

        public void setVideoPreview(ImageView imageView){
            this.videoPreview = imageView;
        }

        public void stopTask(){
            try {
                cancel(true);
                isRunning = false;
                if(clientSocket.isConnected())
                    clientSocket.close();
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
                            if (header.equals("#@@#")) { //VIDEO
                                int imgLength = is.readInt();
                                is.readUTF();
                                byte[] buffers = new byte[imgLength];
                                int len = 0;
                                while (len < imgLength) {
                                    len += is.read(buffers, len, imgLength - len);
                                }
                                Bitmap bitmap = BitmapFactory.decodeByteArray(buffers, 0, buffers.length);
                                getServerActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(videoPreview != null)
                                            videoPreview.setImageBitmap(bitmap);
                                        else
                                            Log.e("VIDEOPREVIEW LOSED","LOSED!");
                                    }
                                });
                            }else // AUDIO
                            if (header.equals("*@@*")) {
                                int audioLength = is.readInt();
                                is.readUTF();
                                byte[] buffers = new byte[audioLength];
                                int len = 0;
                                while (len < audioLength) {
                                    len += is.read(buffers, len, audioLength - len);
                                }
                                getServerActivity().runOnUiThread(new Runnable() {
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

    public void aceptarCliente(String usuario, String id) {

        JsonObject solicitudJson = new JsonObject();
        solicitudJson.addProperty("action", "ok");
        solicitudJson.addProperty("id", id);
        solicitudJson.addProperty("usuario", usuario);


        for (AlertaCard alerta : listaAlertas) {
            if (alerta.getId().equals(id)) {
                listaAlertas.remove(alerta);
                ServerFragmentList.refreshAlertList();
                AcceptClient acceptClient = new AcceptClient(solicitudJson.toString());
                new Thread(acceptClient).start();
                break;
            }
        }
    }

    public void eliminarCliente(String solicitudJson) {
        DeleteClient deleteClient = new DeleteClient(solicitudJson);
        new Thread(deleteClient).start();
    }


    public class AcceptClient implements Runnable{
        private String _id;
        private String _soliciturJson;
        private String _usuario;

        AcceptClient(String solicitudJson) {
            JsonObject jsonObject = JsonParser.parseString(solicitudJson).getAsJsonObject();
            this._usuario = jsonObject.get("usuario").getAsString();
            this._id = jsonObject.get("id").getAsString();
            this._soliciturJson = solicitudJson;
        }

        @Override
        public void run() {

            for (ServerThread serverThread : serverThreads) {
                if (serverThread.getId().equals(_id)) {
                    Socket socket = serverThread.getSocket();
                    try {

                        output = new PrintWriter(socket.getOutputStream(), true);
                        Log.d("Accepted Client", _soliciturJson);

                        output.println(_soliciturJson);
                        output.flush();

                        Bundle bundle = new Bundle();

                        bundle.putString("usuario", _usuario);
                        bundle.putString("IP","Dirección IP: " + SERVER_IP);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ServerFragmentMain fragment = new ServerFragmentMain();
                                fragment.setArguments(bundle);
                                FragmentManager fragmentManager = getSupportFragmentManager();
                                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                                fragmentTransaction.replace(R.id.nav_host_fragment_content_main, fragment);
                                fragmentTransaction.commit();
                                fragmentManager.executePendingTransactions();
                                ImageView imageView = fragment.getVideoView();

                                if (imageView != null) {
                                    serverThread.setVideoPreview(imageView);
                                    audioServer = new AudioServer(socket,getServerActivity());
                                    audioServer.startStream();
                                } else {
                                    Log.e("ERROR FRAGMENT", "Error al obtener el fragmento ServerFragmentMain");
                                }
                            }
                        });


                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void handleDestroy(){
        for (ServerThread serverThread : serverThreads) {
            Socket socket = serverThread.getSocket();
            try {
                if(socket.isConnected()){
                    output = new PrintWriter(socket.getOutputStream(), true);
                    JsonObject _solicitudJson = new JsonObject();
                    _solicitudJson.addProperty("action", "close");
                    _solicitudJson.addProperty("id", serverThread.getId());
                    String _sSolicitudJson = _solicitudJson.toString();
                    output.println(_sSolicitudJson);
                    output.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            serverThread.stopTask();
        }

        isRunning =false;



        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(mainSocketThread != null && mainSocketThread.isAlive()){
            mainSocketThread.interrupt();
        }
        serverThreads.clear();
        listaAlertas.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handleDestroy();

    }

    public class DeleteClient implements Runnable {
        private String _id;
        private String _soliciturJson;

        DeleteClient(String solicitudJson) {
            JsonObject jsonObject = JsonParser.parseString(solicitudJson).getAsJsonObject();
            this._id = jsonObject.get("id").getAsString();
            this._soliciturJson = solicitudJson;
        }
        @Override
        public void run() {

            for (ServerThread serverThread : serverThreads) {
                if (serverThread.getId().equals(_id)) {
                    Socket socket = serverThread.getSocket();
                    try {
                        output = new PrintWriter(socket.getOutputStream(), true);
                        Log.d("Deleted Client", _soliciturJson);
                        output.println(_soliciturJson);
                        output.flush();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                serverThread.stopTask();
                                serverThreads.remove(serverThread);
                                for (Iterator<AlertaCard> iterator = listaAlertas.iterator(); iterator.hasNext();) {
                                    AlertaCard alerta = iterator.next();
                                    if (alerta.getId().equals(_id)) {
                                        iterator.remove();
                                        ServerFragmentList.refreshAlertList();
                                        break;
                                    }
                                }
                            }
                        });
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}