package com.example.workflow;

import com.example.workflow.model.Bodega;
import com.example.workflow.model.SedeEnvio;
import com.example.workflow.repositorio.RepositorioLogistico;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.spin.json.SpinJsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CalculoEnvio implements JavaDelegate {

    @Autowired
    private RepositorioLogistico repositorioLogistico;

    @Override
    public void execute(DelegateExecution execution) {
        String tipoEntrega = (String) execution.getVariable("tipoEntrega");
        boolean esInternacional = "internacional".equalsIgnoreCase(tipoEntrega);

        // ✅ Convertir Spin JSON array a List<String>
        Object raw = execution.getVariable("productos");
        List<String> productos = new ArrayList<>();

        if (raw instanceof SpinJsonNode jsonNode) {
            jsonNode.elements().forEach(node -> productos.add(node.stringValue()));
        }

        SedeEnvio sede = repositorioLogistico.getSede(esInternacional);

        Map<String, Integer> precios = Map.of(
                "ps5", 500,
                "batidora", 30,
                "mochila", 80,
                "tenis", 120,
                "medias", 2
        );

        int total = 0;

        for (String producto : productos) {
            Optional<Bodega> bodegaOpt = repositorioLogistico.encontrarBodegaCercana(producto, esInternacional);
            if (bodegaOpt.isPresent()) {
                Bodega bodega = bodegaOpt.get();
                bodega.descontarProducto(producto);

                total += precios.getOrDefault(producto, 0);
                execution.setVariable("bodega_" + producto, bodega.getNombre());

                System.out.println("✅ Producto '" + producto + "' será enviado desde: " + bodega.getNombre());
            } else {
                System.out.println("❌ Producto '" + producto + "' no disponible en ninguna bodega.");
            }
        }

        int costoEnvio = esInternacional ? 50 : 20;
        execution.setVariable("precioTotalProductos", total);
        execution.setVariable("costoEnvio", costoEnvio);
        execution.setVariable("totalPagar", total + costoEnvio);
        execution.setVariable("sedeEnvio", sede.getNombre());

        System.out.println("✅ Sede de envío: " + sede.getNombre());
        System.out.println("Subtotal: $" + total + " | Envío: $" + costoEnvio + " | Total: $" + (total + costoEnvio));
    }
}
