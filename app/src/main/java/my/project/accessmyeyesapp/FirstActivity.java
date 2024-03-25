package my.project.accessmyeyesapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import java.net.ServerSocket;

public class FirstActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ImageButton btnVoluntario = (ImageButton) findViewById(R.id.btnSolicitante);
        ImageButton btnAprobante = (ImageButton) findViewById(R.id.btnAprobar);

        btnVoluntario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
               startActivity(intent);
            }
        });

        btnAprobante.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ServerActivity.class);
                startActivity(intent);
            }
        });

    }
}