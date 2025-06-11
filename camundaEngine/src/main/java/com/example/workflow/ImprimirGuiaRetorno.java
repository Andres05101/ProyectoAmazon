package com.example.workflow;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ImprimirGuiaRetorno implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String guiaEnvio = "GUIA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        execution.setVariable("guiaEnvio", guiaEnvio);

        String nombreComprador = (String) execution.getVariable("nombre");

        System.out.println("========= GUÍA DE DEVOLUCIÓN =========");
        System.out.println("Guía #: " + guiaEnvio);
        System.out.println("Remitente: " + nombreComprador);
        System.out.println("Destinatario: Amazon");
        System.out.println("Costo: Al cobro");
        System.out.println("=======================================");
    }
}
