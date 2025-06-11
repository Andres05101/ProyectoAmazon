package com.example.workflow;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcesoController {

    @Autowired
    private RuntimeService runtimeService;

    @PostMapping("/iniciar-compra")
    public ResponseEntity<String> iniciarCompra() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("amazon_compra");
        return ResponseEntity.ok("Proceso iniciado con ID: " + instance.getId());
    }
}
