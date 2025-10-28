package com.worksafepro;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOcorrencias = findViewById(R.id.btnOcorrencias);
        Button btnEPI = findViewById(R.id.btnEPI);
        Button btnTreinamentos = findViewById(R.id.btnTreinamentos);
        Button btnRelatorios = findViewById(R.id.btnRelatorios);
        Button btnAgenda = findViewById(R.id.btnAgenda);
        Button btnPerfil = findViewById(R.id.btnPerfil);

        btnOcorrencias.setOnClickListener(v -> startActivity(new Intent(this, OcorrenciasActivity.class)));
        btnEPI.setOnClickListener(v -> startActivity(new Intent(this, EPIActivity.class)));
        btnTreinamentos.setOnClickListener(v -> startActivity(new Intent(this, TreinamentosActivity.class)));
        btnRelatorios.setOnClickListener(v -> startActivity(new Intent(this, RelatoriosActivity.class)));
        btnAgenda.setOnClickListener(v -> startActivity(new Intent(this, AgendaActivity.class)));
        btnPerfil.setOnClickListener(v -> startActivity(new Intent(this, PerfilActivity.class)));
    }
}
