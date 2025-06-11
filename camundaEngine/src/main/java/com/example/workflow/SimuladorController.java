package com.example.workflow;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class SimuladorController {

    @Autowired
    private RuntimeService runtimeService;

    @GetMapping("/simular-supervisor")
    public String simularSupervisor(@RequestParam String businessKey) {
        try {
            runtimeService
                .createMessageCorrelation("mensaje_supervisor")
                .processInstanceBusinessKey(businessKey)
                .correlate();
            return "✅ Mensaje 'mensaje_supervisor' enviado exitosamente.";
        } catch (Exception e) {
            return "⚠️ Error al enviar el mensaje: " + e.getMessage();
        }
    }
}