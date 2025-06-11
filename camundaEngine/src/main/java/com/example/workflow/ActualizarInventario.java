package com.example.workflow;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class ActualizarInventario implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String producto = (String) execution.getVariable("producto");
        Object cantidadInventarioObj = execution.getVariable("cantidadInventario");
        Object cantidadAComprarObj = execution.getVariable("cantidadAComprar");

        int cantidadInventario = 0;
        int cantidadAComprar = 0;

        if (cantidadInventarioObj instanceof Number) {
            cantidadInventario = ((Number) cantidadInventarioObj).intValue();
        } else if (cantidadInventarioObj instanceof String) {
            try {
                cantidadInventario = Integer.parseInt((String) cantidadInventarioObj);
            } catch (Exception e) {
                cantidadInventario = 0;
            }
        }

        if (cantidadAComprarObj instanceof Number) {
            cantidadAComprar = ((Number) cantidadAComprarObj).intValue();
        } else if (cantidadAComprarObj instanceof String) {
            try {
                cantidadAComprar = Integer.parseInt((String) cantidadAComprarObj);
            } catch (Exception e) {
                cantidadAComprar = 0;
            }
        }

        int nuevoInventario = cantidadInventario + cantidadAComprar;
        execution.setVariable("cantidadInventario", nuevoInventario);

        System.out.println("Producto: " + producto);
        System.out.println("Nuevo nivel de inventario: " + nuevoInventario);
    }
}