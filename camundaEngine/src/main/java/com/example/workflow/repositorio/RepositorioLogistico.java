package com.example.workflow.repositorio;

import com.example.workflow.model.Bodega;
import com.example.workflow.model.SedeEnvio;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.*;

@Component
public class RepositorioLogistico {

    private final List<Bodega> bodegas = new ArrayList<>();
    private SedeEnvio sedeNacional;
    private SedeEnvio sedeInternacional;

    @PostConstruct
    public void init() {
        bodegas.add(new Bodega("Bodega Norte", 20));
        bodegas.get(0).agregarProducto("ps5", 2);
        bodegas.get(0).agregarProducto("batidora", 5);

        bodegas.add(new Bodega("Bodega Sur", 70));
        bodegas.get(1).agregarProducto("mochila", 3);
        bodegas.get(1).agregarProducto("ps5", 1);

        bodegas.add(new Bodega("Bodega Central", 50));
        bodegas.get(2).agregarProducto("tenis", 6);
        bodegas.get(2).agregarProducto("medias", 10);

        sedeNacional = new SedeEnvio("Sede Nacional", 30);
        sedeInternacional = new SedeEnvio("Sede Internacional", 90);
    }

    public Optional<Bodega> encontrarBodegaCercana(String producto, boolean internacional) {
        int ubicacionSede = internacional ? sedeInternacional.getUbicacion() : sedeNacional.getUbicacion();
        return bodegas.stream()
                .filter(b -> b.tieneProducto(producto))
                .min(Comparator.comparingInt(b -> Math.abs(b.getUbicacion() - ubicacionSede)));
    }

    public SedeEnvio getSede(boolean internacional) {
        return internacional ? sedeInternacional : sedeNacional;
    }
}
