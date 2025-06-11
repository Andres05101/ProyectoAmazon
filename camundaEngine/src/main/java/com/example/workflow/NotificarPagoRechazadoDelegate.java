package com.example.workflow;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("notificarPagoRechazadoDelegate")
public class NotificarPagoRechazadoDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String cliente = (String) execution.getVariable("cliente");
        Double total = (Double) execution.getVariable("montoCompra");

        System.out.println("⚠️ Pago rechazado para el cliente: " + cliente);
        System.out.println("Monto: $" + total);

        // Simular una notificación (email, mensaje, etc.)
        execution.setVariable("notificacionPagoRechazadoEnviada", true);
    }
}
