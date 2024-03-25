package my.project.accessmyeyesapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LoginActivity extends AppCompatActivity {

    Button btnIniciar;
    EditText txtNombreUsuario, txtServidor;
    TextView lblError;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        btnIniciar = findViewById(R.id.btnIniciarSesion);
        txtNombreUsuario = (EditText) findViewById(R.id.txtUserName);
        txtServidor = (EditText) findViewById(R.id.txtIPServidor);
        lblError = findViewById(R.id.lblErrorConexionServer);

        btnIniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String nombreUsuario = txtNombreUsuario.getText().toString().trim();
                final String servidor = txtServidor.getText().toString().trim();
                if (nombreUsuario.isEmpty() || servidor.isEmpty()) {
                } else {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    final Future<?> future = executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Socket socket = new Socket();
                                socket.connect(new InetSocketAddress(servidor, 12321), 3000);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        txtNombreUsuario.setBackgroundTintList(null);
                                        txtServidor.setBackgroundTintList(null);
                                        lblError.setVisibility(View.INVISIBLE);
                                        Intent intent = new Intent(getApplicationContext(), ClientActivity.class);
                                        intent.putExtra("user", nombreUsuario);
                                        intent.putExtra("servidor", servidor);
                                        try {
                                            socket.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        startActivity(intent);
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        lblError.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }
                    });
                    executor.shutdown();
                    try {
                        future.get(3, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lblError.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            }
        });

    }
}