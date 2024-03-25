package my.project.accessmyeyesapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import androidx.annotation.NonNull;
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
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
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
    public ConstraintLayout layout;
    private Boolean isRunning = true;
    private VideoServer videoServer;
    private SurfaceHolder surfaceHolder;
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
        Log.d("LOYOUT ?", String.valueOf(layout.getId()));
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
            serverSocket = new ServerSocket(12321);
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
                                        String idToRemove = jsonObject.get("id").getAsString();
                                        Iterator<ServerThread> iterator = serverThreads.iterator();
                                        while (iterator.hasNext()) {
                                            ServerThread serverThread = iterator.next();
                                            if (serverThread.getId().equals(idToRemove)) {
                                                serverThread.cancel(true);
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
    private Bitmap base64ToBitmap(String imagenBase64) {
        byte[] decodedString = Base64.decode(imagenBase64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    private PrintWriter output;
    private BufferedReader input;

    class ServerThread extends AsyncTask<Void, String, Void> {

        private Socket clientSocket;
        private String _id;

        public ServerThread(Socket clientSocket, String id) {
            this.clientSocket = clientSocket;
            this._id = id;
        }

        public void stopTask(){
            cancel(true);
        }

        public String getId(){
            return this._id;
        }
        public Socket getSocket() {return this.clientSocket;}
        @Override
        protected Void doInBackground(Void... params) {
            /*try (BufferedReader dis = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            //try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
                while (true) {
                    String imagenBase64 = dis.readLine();
                    Log.d("IMAGE ?", imagenBase64);

                    if (imagenBase64 != null) {
                        Bitmap bitmap = base64ToBitmap(imagenBase64);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
            return null;
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

                                if (fragment != null) {
                                    SurfaceView surfaceView = fragment.getVideoView();
                                    videoServer = new VideoServer(socket, surfaceView);
                                    videoServer.iniciarRecepcion();
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
                output = new PrintWriter(socket.getOutputStream(), true);
                JsonObject _solicitudJson = new JsonObject();
                _solicitudJson.addProperty("action", "close");
                _solicitudJson.addProperty("id", serverThread.getId());
                String _sSolicitudJson = _solicitudJson.toString();
                output.println(_sSolicitudJson);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (ServerThread serverThread : serverThreads) {
            serverThread.stopTask();
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(mainSocketThread.isAlive()){
            mainSocketThread.interrupt();
        }
        serverThreads.clear();
        listaAlertas.clear();
        isRunning =false;
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