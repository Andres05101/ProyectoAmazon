package com.example.workflow;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.spin.json.SpinJsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class GenerarFactura implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {

        String idPedido = UUID.randomUUID().toString();
        execution.setVariable("idPedido", idPedido); 

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

        int precioTotal = (int) execution.getVariable("precioTotalProductos");
        int costoEnvio = (int) execution.getVariable("costoEnvio");
        int totalPagar = (int) execution.getVariable("totalPagar");

        System.out.println("========= FACTURA DE COMPRA =========");
        System.out.println("ID de Pedido: " + idPedido);
        System.out.println("Cliente: " + nombre);
        System.out.println("Dirección: " + direccion + ", " + ciudad + ", " + estado + ", " + pais);
        System.out.println("Tipo de entrega: " + tipoEntrega.toUpperCase());
        System.out.println("Sede logística: " + sedeEnvio);
        System.out.println("Productos comprados:");
        for (String producto : productos) {
            String bodega = (String) execution.getVariable("bodega_" + producto);
            System.out.println(" - " + producto + " (desde " + bodega + ")");
        }
        System.out.println("-------------------------------------");
        System.out.println("Subtotal: $" + precioTotal);
        System.out.println("Costo de envío: $" + costoEnvio);
        System.out.println("TOTAL A PAGAR: $" + totalPagar);
        System.out.println("=====================================");

        execution.setVariable("totalPagar", totalPagar);
    }
}
