package com.clienteapp.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import com.clienteapp.services.CamundaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class ClienteController {

    private static final Logger logger = LoggerFactory.getLogger(ClienteController.class);

    @Autowired
    private CamundaService camundaService;

    private final String clienteUserId = "cliente1"; // para pruebas

    @GetMapping("/")
    public String mostrarInicio() {
        return "index"; // Carga el index.html de Thymeleaf
    }    

    @GetMapping("/tareas")
    public String verTareas(Model model) {
        CamundaService.Task[] tasks = camundaService.getTasksByAssignee(clienteUserId);
        model.addAttribute("tasks", tasks);
        return "tareas"; // plantilla Thymeleaf tareas.html
    }

    @PostMapping("/completar")
    public String completarTarea(@RequestParam String taskId) {
        camundaService.completeTask(taskId);
        return "redirect:/tareas";
    }

    @GetMapping("/formulario-inicio")
    public String mostrarFormulario(@RequestParam(name = "proceso") String proceso, Model model) {
        String formKey = camundaService.getStartFormKey(proceso);
        model.addAttribute("formKey", formKey);
        model.addAttribute("proceso", proceso);
        if (proceso != null && (proceso.trim().equalsIgnoreCase("Reposiciones_Amazon") || proceso.trim().equalsIgnoreCase("Amazon_Reposicion"))) {
            return "nivel_inventario";
        } else if (proceso != null && proceso.trim().equalsIgnoreCase("Devoluciones_Amazon")) {
            return "solicitud-cliente";
        } else {
            // Puedes agregar más procesos y sus vistas aquí
        return "solicitud-cliente"; 
        }
    }

    @PostMapping("/start-process")
    public String iniciarProceso(@RequestParam Map<String, String> allParams, Model model) {
        try {
            Map<String, Object> variables = new HashMap<>();
            String proceso = allParams.get("proceso");
            if ("Reposiciones_Amazon".equals(proceso) || "Amazon_Reposicion".equals(proceso)) {
                variables.put("producto", allParams.get("producto"));
                variables.put("cantidadInventario", allParams.get("cantidad"));
                variables.put("stockMinimo", allParams.get("stockMinimo"));
            } else {
                variables.put("nombre", allParams.get("nombre"));
                String fechaStr = allParams.get("fechaCompra");
                if (fechaStr != null && !fechaStr.isEmpty()) {
                    try {
                        Date fecha = new SimpleDateFormat("yyyy-MM-dd").parse(fechaStr);
                        variables.put("fechaCompra", fecha);
                    } catch (Exception e) {
                        logger.error("Error de formato en fechaCompra: {}", fechaStr);
                        model.addAttribute("mensaje", "La fecha de compra no es válida. Usa el formato yyyy-MM-dd.");
                        return "error";
                    }
                }
                variables.put("motivo", allParams.get("motivo"));
            }
            logger.info("Iniciando proceso {} con variables: {}", proceso, variables);
            String instanciaId = camundaService.startProcessWithVariables(proceso, variables);
            if ("Reposiciones_Amazon".equals(proceso) || "Amazon_Reposicion".equals(proceso)) {
                camundaService.enviarMensajeReponerStock(instanciaId);
            } else if ("Devoluciones_Amazon".equals(proceso)) {
                camundaService.enviarMensajeAprobacionSupervisor(instanciaId);
            }
            model.addAttribute("mensaje", "Proceso iniciado y mensaje de aprobación enviado automáticamente.");
            return "proceso-iniciado";
        } catch (Exception e) {
            logger.error("Error al iniciar proceso: {}", e.getMessage());
            model.addAttribute("mensaje", "Error: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/aprobar-supervisor")
    public String aprobarSupervisor(@RequestParam String instancia, Model model) {
        try {
            camundaService.enviarMensajeAprobacionSupervisor(instancia);
            model.addAttribute("mensaje", "Mensaje de aprobación enviado correctamente.");
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al enviar mensaje: " + e.getMessage());
        }
        return "proceso-iniciado";
    }

    @PostMapping("/iniciar-compra")
    public String iniciarCompra(
        @RequestParam Map<String, String> allParams,
        @RequestParam(name = "productos", required = false) String[] productos,
        Model model) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("nombre", allParams.get("nombre"));
            variables.put("pais", allParams.get("pais"));
            variables.put("estado", allParams.get("estado"));
            variables.put("ciudad", allParams.get("ciudad"));
            variables.put("direccion", allParams.get("direccion"));
            variables.put("tipoEntrega", allParams.get("tipoEntrega"));
            if (productos != null) {
                ObjectMapper mapper = new ObjectMapper();
                String productosJson = mapper.writeValueAsString(productos);
                variables.put("productos", productosJson);
            } else {
                variables.put("productos", "[]");
            }
            String proceso = "Compras_Amazon";
            logger.info("Iniciando proceso {} con variables: {}", proceso, variables);
            String instanciaId = camundaService.startProcessWithVariables(proceso, variables);

            // Buscar tareas pendientes para el grupo 'clientes' en esta instancia
            String grupoCliente = "clientes";
            CamundaService.Task[] tareas = camundaService.getTasksByCandidateGroupAndInstance(grupoCliente, instanciaId);
            if (tareas != null && tareas.length > 0) {
                String taskId = tareas[0].getId();
                // Obtener el formKey de la tarea
                String formKeyUrl = "http://localhost:8080/engine-rest/task/" + taskId + "/form";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                HttpEntity<?> request = new HttpEntity<>(headers);
                RestTemplate restTemplate = new RestTemplate();
                org.springframework.http.ResponseEntity<java.util.Map> formKeyResponse = restTemplate.exchange(formKeyUrl, org.springframework.http.HttpMethod.GET, request, java.util.Map.class);
                java.util.Map formKeyBody = formKeyResponse.getBody();
                if (formKeyBody != null && formKeyBody.containsKey("key")) {
                    String formKey = (String) formKeyBody.get("key");
                    if (formKey != null && formKey.startsWith("embedded:app:forms/") && formKey.endsWith(".html")) {
                        String viewName = formKey.substring("embedded:app:forms/".length(), formKey.length() - ".html".length());
                        model.addAttribute("taskId", taskId);
                        model.addAttribute("processInstanceId", instanciaId);

                        // Obtener variables de Camunda si la vista lo requiere
                        if ("formulario_pago".equals(viewName)) {
                            Map<String, Object> variablesProceso = camundaService.getProcessVariables(instanciaId);
                            if (variablesProceso != null && variablesProceso.containsKey("totalPagar")) {
                                Object totalPagarObj = variablesProceso.get("totalPagar");
                                if (totalPagarObj instanceof Map) {
                                    Object value = ((Map<?, ?>) totalPagarObj).get("value");
                                    model.addAttribute("totalPagar", value != null ? value.toString() : "");
                                } else {
                                    model.addAttribute("totalPagar", totalPagarObj.toString());
                                }
                            } else {
                                model.addAttribute("totalPagar", "");
                            }
                        }
                        return viewName;
                    }
                }
                String taskName = tareas[0].getName().toLowerCase();
                model.addAttribute("taskId", tareas[0].getId());
                model.addAttribute("taskName", tareas[0].getName());
                // Selección automática de plantilla según el nombre de la tarea
                if (taskName.contains("pago")) {
                    // Obtener variables del proceso para el campo totalPagar
                    Map<String, Object> variablesProceso = camundaService.getProcessVariables(instanciaId);
                    if (variablesProceso != null && variablesProceso.containsKey("totalPagar")) {
                        Object totalPagarObj = variablesProceso.get("totalPagar");
                        if (totalPagarObj instanceof Map) {
                            Object value = ((Map<?, ?>) totalPagarObj).get("value");
                            model.addAttribute("totalPagar", value != null ? value.toString() : "");
                        } else {
                            model.addAttribute("totalPagar", totalPagarObj.toString());
                        }
                    } else {
                        model.addAttribute("totalPagar", "");
                    }
                    return "formulario_pago";
                } else {
                    return "formulario-tarea-cliente";
                }
            }

            model.addAttribute("mensaje", "Compra iniciada correctamente. ID de instancia: " + instanciaId);
            return "proceso-iniciado";
        } catch (Exception e) {
            logger.error("Error al iniciar compra: {}", e.getMessage());
            model.addAttribute("mensaje", "Error: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/registro-compras")
    public String mostrarRegistroCompras() {
        return "registro_compras";
    }

    @PostMapping("/completar-tarea-pago")
    public String completarTareaPago(
        @RequestParam("taskId") String taskId,
        @RequestParam("numTarjeta") String numTarjeta,
        @RequestParam("fecExpiracion") String fecExpiracion,
        @RequestParam("codCVV") String codCVV,
        @RequestParam("totalPagar") String totalPagar,
        @RequestParam("processInstanceId") String processInstanceId,
        Model model
    ) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("numTarjeta", numTarjeta);
            variables.put("fecExpiracion", fecExpiracion);
            variables.put("codCVV", codCVV);
            variables.put("totalPagar", totalPagar);

            camundaService.completeTaskWithVariables(taskId, variables);

            // Buscar la siguiente tarea de la misma instancia
            String grupoCliente = "clientes";
            CamundaService.Task[] tareas = camundaService.getTasksByCandidateGroupAndInstance(grupoCliente, processInstanceId);
            if (tareas != null && tareas.length > 0) {
                String nextTaskId = tareas[0].getId();
                String nextTaskName = tareas[0].getName();
                model.addAttribute("taskId", nextTaskId);
                model.addAttribute("taskName", nextTaskName);
                model.addAttribute("processInstanceId", processInstanceId);
                return "formulario-tarea-cliente";
            }
            model.addAttribute("mensaje", "Pago realizado correctamente. No hay más tareas pendientes en esta instancia.");
            return "proceso-iniciado";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al procesar el pago: " + e.getMessage());
            return "error";
        }
    }

}
