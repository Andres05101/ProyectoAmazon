package com.example.workflow;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class NotificarCondicion implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String condicion = (String) execution.getVariable("condicionProducto");
        System.out.println("📦 Condición del producto recibida: " + condicion);
    }
}