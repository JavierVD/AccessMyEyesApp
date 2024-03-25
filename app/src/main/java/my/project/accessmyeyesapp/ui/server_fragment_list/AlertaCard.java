package my.project.accessmyeyesapp.ui.server_fragment_list;

import java.util.ArrayList;

public class AlertaCard {
    private String _nombre;
    private String _id;

    public AlertaCard(String nombre, String id) {
        _nombre = nombre;
        _id = id;
    }

    public String getNombre() {
        return _nombre;
    }

    public String getId() {
        return _id;
    }
}
