package my.project.accessmyeyesapp.ui.server_fragment_list;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;

import java.util.List;

import my.project.accessmyeyesapp.R;
import my.project.accessmyeyesapp.ServerActivity;
import my.project.accessmyeyesapp.ui.server_fragment_main.ServerFragmentMain;

public class AlertasAdapter extends RecyclerView.Adapter<AlertasAdapter.ViewHolder> {

    private List<AlertaCard> _alertas;
    private Context _context;

    public AlertasAdapter(List<AlertaCard> alertas, Context context) {
        _alertas = alertas;
        _context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.server_fragment_list_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AlertaCard alerta = _alertas.get(position);
        String cardId = alerta.getId();
        holder.nombreClienteCard.setText(alerta.getNombre());
        holder.btnAceptar.setOnClickListener(v -> {
            ((ServerActivity)_context).aceptarCliente(alerta.getNombre(), alerta.getId());
        });
        holder.btnRechazar.setOnClickListener(v -> {
            String solicitudJson = crearSolicitud(cardId, 0);
            ((ServerActivity)_context).eliminarCliente(solicitudJson);
        });
    }

    @Override
    public int getItemCount() {
        return _alertas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nombreClienteCard;
        public Button btnAceptar, btnRechazar;
        public ImageView imagenCard;
        public ViewHolder(View itemView) {
            super(itemView);
            btnAceptar = itemView.findViewById(R.id.btnAceptarClienteCard);
            btnRechazar = itemView.findViewById(R.id.btnRechazarClienteCard);
            nombreClienteCard = itemView.findViewById(R.id.nombreClienteCard);
            imagenCard = itemView.findViewById(R.id.imagenAlertaCard);
        }
    }

    private String crearSolicitud(String cardId, int action) {
        JsonObject solicitudJson = new JsonObject();
        solicitudJson.addProperty("action", action == 0 ? "cancel" : "accept");
        solicitudJson.addProperty("id", cardId);
        return solicitudJson.toString();
    }
}