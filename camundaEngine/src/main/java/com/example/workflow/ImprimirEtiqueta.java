package com.example.workflow;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.spin.json.SpinJsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ImprimirEtiqueta implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String nombre = (String) execution.getVariable("nombre");
        String pais = (String) execution.getVariable("pais");
        String estado = (String) execution.getVariable("estado");
        String ciudad = (String) execution.getVariable("ciudad");
        String direccion = (String) execution.getVariable("direccion");
        String tipoEntrega = (String) execution.getVariable("tipoEntrega");
        String sedeEnvio = (String) execution.getVariable("sedeEnvio");

        Object raw = execution.getVariable("productos");
        List<String> productos = new ArrayList<>();
        if (raw instanceof SpinJsonNode jsonNode) {
            jsonNode.elements().forEach(node -> productos.add(node.stringValue()));
        }

        int costoEnvio = (int) execution.getVariable("costoEnvio");
        String guiaEnvio = "GUIA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        execution.setVariable("guiaEnvio", guiaEnvio);

        System.out.println("========= ETIQUETA DE ENVÍO =========");
        System.out.println("Guía #: " + guiaEnvio);
        System.out.println("Destinatario: " + nombre);
        System.out.println("Dirección: " + direccion + ", " + ciudad + ", " + estado + ", " + pais);
        System.out.println("Tipo entrega: " + tipoEntrega.toUpperCase());
        System.out.println("Sede de salida: " + sedeEnvio);
        System.out.println("Productos enviados:");
        for (String producto : productos) {
            String bodega = (String) execution.getVariable("bodega_" + producto);
            System.out.println(" - " + producto + " (desde " + bodega + ")");
        }
        System.out.println("Costo del envío: $" + costoEnvio);
        System.out.println("=====================================");
    }
}
