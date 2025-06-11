package com.example.workflow;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class CrearOrdenCompra implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String producto = (String) execution.getVariable("producto");
        Number cantidadInventarioNum = (Number) execution.getVariable("cantidadInventario");
        Number stockMinimoNum = (Number) execution.getVariable("stockMinimo");
        int cantidadInventario = cantidadInventarioNum != null ? cantidadInventarioNum.intValue() : 0;
        int stockMinimo = stockMinimoNum != null ? stockMinimoNum.intValue() : 0;

        // Calcular la cantidad a comprar para cubrir el stock mínimo
        int cantidadAComprar = Math.max(stockMinimo - cantidadInventario, 0);

        // Seleccionar aleatoriamente un proveedor
        String[] proveedores = {"Proveedor 1", "Proveedor 2", "Proveedor 3"};
        String proveedor = proveedores[(int) (Math.random() * proveedores.length)];

        // Generar una ID de orden de forma simple
        String ordenCompraId = "OC-" + System.currentTimeMillis();

        // Guardar variables en el proceso
        execution.setVariable("ordenCompraId", ordenCompraId);
        execution.setVariable("proveedor", proveedor);
        execution.setVariable("productoOrdenado", producto);
        execution.setVariable("cantidadOrdenada", cantidadAComprar);
        execution.setVariable("cantidadAComprar", cantidadAComprar);

        // Imprimir la orden de compra
        System.out.println("Orden de compra creada:");
        System.out.println("ID: " + ordenCompraId);
        System.out.println("Proveedor: " + proveedor);
        System.out.println("Producto: " + producto);
        System.out.println("Cantidad en inventario actual: " + cantidadInventario);
        System.out.println("Stock mínimo requerido: " + stockMinimo);
        System.out.println("Cantidad a comprar: " + cantidadAComprar);
    }
}
