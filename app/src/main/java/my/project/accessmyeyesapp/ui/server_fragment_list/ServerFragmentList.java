package my.project.accessmyeyesapp.ui.server_fragment_list;

import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;

import my.project.accessmyeyesapp.R;
import my.project.accessmyeyesapp.databinding.ServerFragmentListBinding;

public class ServerFragmentList extends Fragment {

    private ServerFragmentListBinding binding;
    private RecyclerView _recyclerAlertas;
    private TextView _lblIP;
    private static AlertasAdapter _adapter;
    public static List<AlertaCard> listaAlertas = new ArrayList<>();
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ServerFragmentViewModel serverFragmentViewModel =
                new ViewModelProvider(this).get(ServerFragmentViewModel.class);

        binding = ServerFragmentListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        _recyclerAlertas = root.findViewById(R.id.recyclerAlertas);
        _lblIP = root.findViewById(R.id.lblIP);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        _recyclerAlertas.setLayoutManager(layoutManager);

        _adapter = new AlertasAdapter(listaAlertas, getContext());
        _recyclerAlertas.setAdapter(_adapter);

        return root;
    }

    public TextView getIPTextView(){
        return _lblIP;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static void refreshAlertList(){
        if (_adapter != null) {
            _adapter.notifyDataSetChanged();
        }
    }

}