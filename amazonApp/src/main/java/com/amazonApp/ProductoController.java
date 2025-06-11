package com.amazonApp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
public class ProductoController {
    private final String camundaUrl = "http://localhost:8080/engine-rest";
    private final String username = "admin";
    private final String password = "123";
    private final RestTemplate restTemplate = new RestTemplate();
    private final String usuarioAmazon = "amazon1";

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = username + ":" + password;
        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }

    @GetMapping({"/", "/siguiente-tarea"})
    public String mostrarSiguienteTarea(Model model) {
        String url = camundaUrl + "/task?candidateGroup=administradores";
        HttpHeaders headers = createHeaders();
        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);
        List<Map<String, Object>> tareas = response.getBody();
        if (tareas != null && !tareas.isEmpty()) {
            for (Map<String, Object> tarea : tareas) {
                String taskId = tarea.get("id").toString();
                String processInstanceId = tarea.get("processInstanceId").toString();
                String taskName = tarea.get("name") != null ? tarea.get("name").toString() : "";
                String formKey = tarea.get("formKey") != null ? tarea.get("formKey").toString() : null;
                if (formKey != null && formKey.startsWith("embedded:app:forms/") && formKey.endsWith(".html")) {
                    String viewName = formKey.substring("embedded:app:forms/".length(), formKey.length() - ".html".length());
                    model.addAttribute("taskId", taskId);
                    model.addAttribute("processInstanceId", processInstanceId);

                    if ("recibir_orden_compra".equals(viewName)) {
                        return "redirect:/recibir-orden-compra?taskId=" + taskId + "&processInstanceId=" + processInstanceId;
                    }
                    if ("registrar_entrada_producto".equals(viewName)) {
                        return "redirect:/registrar-entrada-producto?taskId=" + taskId + "&processInstanceId=" + processInstanceId;
                    }

                    Map<String, Object> variables = obtenerVariablesDeProceso(processInstanceId);

                    if ("validar_credenciales_compra".equals(viewName)) {
                        model.addAttribute("numTarjeta", variables.getOrDefault("numTarjeta", ""));
                        model.addAttribute("idPedido", variables.getOrDefault("idPedido", ""));
                        model.addAttribute("fecExpiracion", variables.getOrDefault("fecExpiracion", ""));
                    }
                    if ("revisar_solicitud".equals(viewName)) {
                        model.addAttribute("producto", variables.getOrDefault("producto", ""));
                        model.addAttribute("cantidadInventario", variables.getOrDefault("cantidadInventario", ""));
                        model.addAttribute("stockMinimo", variables.getOrDefault("stockMinimo", ""));
                    }

                    return viewName;
                }
            }
        }
        model.addAttribute("mensaje", "No hay tareas pendientes para este grupo.");
        return "mensaje";
    }

    @PostMapping("/validar-condiciones-producto")
    public String procesarFormularioCondiciones(
        @RequestParam(name = "condicionProducto") String condicionProducto,
        @RequestParam(name = "consolidarEstado") String consolidarEstado,
        @RequestParam(name = "taskId") String taskId,
        @RequestParam(name = "processInstanceId") String processInstanceId,
        Model model) {
        String url = camundaUrl + "/task/" + taskId + "/complete";
        HttpHeaders headers = createHeaders();
        Map<String, Object> variables = new HashMap<>();
        Map<String, Object> condicion = new HashMap<>();
        condicion.put("value", condicionProducto);
        condicion.put("type", "String");
        variables.put("condicionProducto", condicion);
        Map<String, Object> estado = new HashMap<>();
        estado.put("value", consolidarEstado);
        estado.put("type", "String");
        variables.put("consolidarEstado", estado);
        Map<String, Object> body = new HashMap<>();
        body.put("variables", variables);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
        return "redirect:/siguiente-tarea?processInstanceId=" + processInstanceId;
    }

    @PostMapping("/validar-credenciales-compra")
    public String procesarValidarCredenciales(
        @RequestParam("taskId") String taskId,
        @RequestParam("processInstanceId") String processInstanceId,
        @RequestParam("resPago") String resPago,
        Model model) {
        String url = camundaUrl + "/task/" + taskId + "/complete";
        HttpHeaders headers = createHeaders();
        Map<String, Object> variables = new HashMap<>();
        Map<String, Object> resPagoVar = new HashMap<>();
        resPagoVar.put("value", resPago);
        resPagoVar.put("type", "String");
        variables.put("resPago", resPagoVar);
        Map<String, Object> body = new HashMap<>();
        body.put("variables", variables);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
        return "redirect:/siguiente-tarea?processInstanceId=" + processInstanceId;
    }

    @PostMapping("/completar-tarea-distribuidor")
    public String procesarFormularioDistribuidor(
        @RequestParam(name = "distriNacional", required = false) String distriNacional,
        @RequestParam(name = "distriInternacional", required = false) String distriInternacional,
        @RequestParam(name = "taskId") String taskId,
        @RequestParam(name = "processInstanceId") String processInstanceId,
        Model model) {
        String url = camundaUrl + "/task/" + taskId + "/complete";
        HttpHeaders headers = createHeaders();
        Map<String, Object> variables = new HashMap<>();
        if (distriNacional != null) {
            Map<String, Object> distribuidor = new HashMap<>();
            distribuidor.put("value", distriNacional);
            distribuidor.put("type", "String");
            variables.put("distriNacional", distribuidor);
        } else if (distriInternacional != null) {
            Map<String, Object> distribuidor = new HashMap<>();
            distribuidor.put("value", distriInternacional);
            distribuidor.put("type", "String");
            variables.put("distriInternacional", distribuidor);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("variables", variables);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        return "redirect:/siguiente-tarea?processInstanceId=" + processInstanceId;
    }

    @GetMapping("/validar-credenciales-compra")
    public String mostrarValidarCredencialesCompra(@RequestParam(name = "processInstanceId") String processInstanceId, Model model) {
        String url = camundaUrl + "/task?candidateGroup=administradores&processInstanceId=" + processInstanceId;
        HttpHeaders headers = createHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Map[]> response = restTemplate.exchange(url, HttpMethod.GET, request, Map[].class);
        Map[] tareas = response.getBody();
        if (tareas != null && tareas.length > 0) {
            for (Map tarea : tareas) {
                String taskName = tarea.get("name").toString().toLowerCase();
                if (taskName.contains("credenciales")) {
                    String taskId = tarea.get("id").toString();
                    model.addAttribute("taskId", taskId);
                    model.addAttribute("processInstanceId", processInstanceId);
                    break;
                }
            }
        }
        Map<String, Object> variables = obtenerVariablesDeProceso(processInstanceId);
        model.addAttribute("numTarjeta", variables.getOrDefault("numTarjeta", ""));
        model.addAttribute("idPedido", variables.getOrDefault("idPedido", ""));
        model.addAttribute("fecExpiracion", variables.getOrDefault("fecExpiracion", ""));
        return "validar_credenciales_compra";
    }

    @PostMapping("/enviar-mensaje-reponer-stock")
    public String enviarMensajeReponerStock(@RequestParam("processInstanceId") String processInstanceId, Model model) {
        String url = camundaUrl + "/message";
        HttpHeaders headers = createHeaders();
        Map<String, Object> body = new HashMap<>();
        body.put("messageName", "ReponerStock");
        body.put("processInstanceId", processInstanceId);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                model.addAttribute("mensaje", "Mensaje 'ReponerStock' enviado correctamente.");
            } else {
                model.addAttribute("mensaje", "Error al enviar mensaje: " + response.getBody());
            }
        } catch (Exception e) {
            model.addAttribute("mensaje", "Excepción al enviar mensaje: " + e.getMessage());
        }
        model.addAttribute("processInstanceId", processInstanceId);
        return "mensaje";
    }

    @GetMapping("/revisar-solicitud")
    public String mostrarRevisarSolicitud(@RequestParam("taskId") String taskId, @RequestParam("processInstanceId") String processInstanceId, Model model) {
        model.addAttribute("taskId", taskId);
        model.addAttribute("processInstanceId", processInstanceId);
        Map<String, Object> variables = obtenerVariablesDeProceso(processInstanceId);
        model.addAttribute("producto", variables.getOrDefault("producto", ""));
        model.addAttribute("cantidadInventario", variables.getOrDefault("cantidadInventario", ""));
        model.addAttribute("stockMinimo", variables.getOrDefault("stockMinimo", ""));
        return "revisar_solicitud";
    }

    @PostMapping("/revisar-solicitud")
    public String procesarRevisarSolicitud(
        @RequestParam("taskId") String taskId,
        @RequestParam("processInstanceId") String processInstanceId,
        @RequestParam("aprobada") Boolean aprobada,
        @RequestParam(value = "observaciones", required = false) String observaciones,
        Model model) {
        String url = camundaUrl + "/task/" + taskId + "/complete";
        HttpHeaders headers = createHeaders();
        Map<String, Object> variables = new HashMap<>();
        Map<String, Object> aprobadaVar = new HashMap<>();
        aprobadaVar.put("value", aprobada);
        aprobadaVar.put("type", "Boolean");
        variables.put("aprobada", aprobadaVar);
        Map<String, Object> observacionesVar = new HashMap<>();
        observacionesVar.put("value", observaciones);
        observacionesVar.put("type", "String");
        variables.put("observaciones", observacionesVar);

        // Recuperar cantidadInventario y stockMinimo del proceso y reenviarlos como Integer
        String urlVars = camundaUrl + "/process-instance/" + processInstanceId + "/variables";
        HttpEntity<?> requestVars = new HttpEntity<>(headers);
        ResponseEntity<Map> responseVars = restTemplate.exchange(urlVars, HttpMethod.GET, requestVars, Map.class);
        Map vars = responseVars.getBody();
        if (vars != null) {
            Object cantidadInventarioObj = vars.get("cantidadInventario");
            Object stockMinimoObj = vars.get("stockMinimo");
            Integer cantidadInventario = null;
            Integer stockMinimo = null;
            if (cantidadInventarioObj instanceof Map) {
                Object value = ((Map) cantidadInventarioObj).get("value");
                if (value instanceof Number) {
                    cantidadInventario = ((Number) value).intValue();
                } else if (value instanceof String) {
                    try { cantidadInventario = Integer.parseInt((String) value); } catch (Exception e) { cantidadInventario = 0; }
                }
            }
            if (stockMinimoObj instanceof Map) {
                Object value = ((Map) stockMinimoObj).get("value");
                if (value instanceof Number) {
                    stockMinimo = ((Number) value).intValue();
                } else if (value instanceof String) {
                    try { stockMinimo = Integer.parseInt((String) value); } catch (Exception e) { stockMinimo = 0; }
                }
            }
            if (cantidadInventario != null) {
                Map<String, Object> cantidadInventarioVar = new HashMap<>();
                cantidadInventarioVar.put("value", cantidadInventario);
                cantidadInventarioVar.put("type", "Integer");
                variables.put("cantidadInventario", cantidadInventarioVar);
            }
            if (stockMinimo != null) {
                Map<String, Object> stockMinimoVar = new HashMap<>();
                stockMinimoVar.put("value", stockMinimo);
                stockMinimoVar.put("type", "Integer");
                variables.put("stockMinimo", stockMinimoVar);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("variables", variables);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
        return "redirect:/siguiente-tarea?processInstanceId=" + processInstanceId;
    }

    @GetMapping("/registrar-entrada-producto")
    public String mostrarRegistrarEntradaProducto(@RequestParam("taskId") String taskId, @RequestParam("processInstanceId") String processInstanceId, Model model) {
        model.addAttribute("taskId", taskId);
        model.addAttribute("processInstanceId", processInstanceId);
        Map<String, Object> variables = obtenerVariablesDeProceso(processInstanceId);
        String producto = variables.getOrDefault("productoOrdenado", variables.get("producto")) != null
            ? variables.getOrDefault("productoOrdenado", variables.get("producto")).toString()
            : "";
        String cantidadAComprar = variables.getOrDefault("cantidadAComprar", "").toString();
        model.addAttribute("producto", producto);
        model.addAttribute("cantidadAComprar", cantidadAComprar);
        return "registrar_entrada_producto";
    }
    @PostMapping("/registrar-entrada-producto")
    public String procesarRegistrarEntradaProducto(
        @RequestParam("taskId") String taskId,
        @RequestParam("processInstanceId") String processInstanceId,
        @RequestParam("producto") String producto,
        @RequestParam("cantidadAComprar") Integer cantidadAComprar,
        @RequestParam("estadoProductos") String estadoProductos,
        Model model) {
        String url = camundaUrl + "/task/" + taskId + "/complete";
        HttpHeaders headers = createHeaders();
        Map<String, Object> variables = new HashMap<>();
        Map<String, Object> estadoVar = new HashMap<>();
        estadoVar.put("value", estadoProductos);
        estadoVar.put("type", "String");
        variables.put("estadoProductos", estadoVar);
        // Reenviar producto y cantidadAComprar
        Map<String, Object> productoVar = new HashMap<>();
        productoVar.put("value", producto);
        productoVar.put("type", "String");
        variables.put("productoOrdenado", productoVar);

        Map<String, Object> cantidadAComprarVar = new HashMap<>();
        cantidadAComprarVar.put("value", cantidadAComprar);
        cantidadAComprarVar.put("type", "Integer");
        variables.put("cantidadAComprar", cantidadAComprarVar);

        Map<String, Object> body = new HashMap<>();
        body.put("variables", variables);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);

        model.addAttribute("producto", producto);
        model.addAttribute("cantidadAComprar", cantidadAComprar);
        return "redirect:/siguiente-tarea?processInstanceId=" + processInstanceId;
    }

    // FUNCIÓN UTILITARIA PARA OBTENER TODAS LAS VARIABLES DEL PROCESO
    private Map<String, Object> obtenerVariablesDeProceso(String processInstanceId) {
        String urlVars = camundaUrl + "/process-instance/" + processInstanceId + "/variables";
        HttpHeaders headers = createHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Map> responseVars = restTemplate.exchange(urlVars, HttpMethod.GET, request, Map.class);
        Map variables = responseVars.getBody();
        Map<String, Object> resultado = new HashMap<>();
        if (variables != null) {
            for (Object keyObj : variables.keySet()) {
                String key = keyObj.toString();
                Object valueObj = variables.get(key);
                Object value = valueObj instanceof Map ? ((Map) valueObj).get("value") : valueObj;
                // Detectar si es una fecha en formato ISO y convertirla a Date
                if (value instanceof String) {
                    String strVal = (String) value;
                    // Detectar formato ISO 8601 (ejemplo: 2024-06-12T00:00:00.000+0000)
                    if (strVal.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4}")) {
                        try {
                            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                            Date fecha = isoFormat.parse(strVal);
                            resultado.put(key, fecha);
                            continue;
                        } catch (ParseException e) {
                            // Si falla el parseo, dejar el valor como String
                        }
                    }
                }
                resultado.put(key, value);
            }
        }
        return resultado;
    }

    @GetMapping("/recibir-orden-compra")
    public String mostrarRecibirOrdenCompra(@RequestParam("taskId") String taskId, @RequestParam("processInstanceId") String processInstanceId, Model model) {
        model.addAttribute("taskId", taskId);
        model.addAttribute("processInstanceId", processInstanceId);
        Map<String, Object> variables = obtenerVariablesDeProceso(processInstanceId);
        String producto = variables.getOrDefault("productoOrdenado", variables.get("producto")) != null
            ? variables.getOrDefault("productoOrdenado", variables.get("producto")).toString()
            : "";
        String cantidadAComprar = variables.getOrDefault("cantidadAComprar", "").toString();
        model.addAttribute("producto", producto);
        model.addAttribute("cantidadAComprar", cantidadAComprar);
        return "recibir_orden_compra";
    }

    @PostMapping("/recibir-orden-compra")
    public String procesarRecibirOrdenCompra(
        @RequestParam("taskId") String taskId,
        @RequestParam("processInstanceId") String processInstanceId,
        @RequestParam("producto") String producto,
        @RequestParam("cantidadAComprar") Integer cantidadAComprar,
        @RequestParam("aceptada") Boolean aceptada,
        Model model) {

        String url = camundaUrl + "/task/" + taskId + "/complete";
        HttpHeaders headers = createHeaders();
        Map<String, Object> variables = new HashMap<>();

        Map<String, Object> aceptadaVar = new HashMap<>();
        aceptadaVar.put("value", aceptada);
        aceptadaVar.put("type", "Boolean");
        variables.put("aceptada", aceptadaVar);

        // Reenviar producto y cantidadAComprar si se necesitan en el proceso
        Map<String, Object> productoVar = new HashMap<>();
        productoVar.put("value", producto);
        productoVar.put("type", "String");
        variables.put("productoOrdenado", productoVar);

        Map<String, Object> cantidadAComprarVar = new HashMap<>();
        cantidadAComprarVar.put("value", cantidadAComprar);
        cantidadAComprarVar.put("type", "Integer");
        variables.put("cantidadAComprar", cantidadAComprarVar);

        Map<String, Object> body = new HashMap<>();
        body.put("variables", variables);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);

        return "redirect:/siguiente-tarea?processInstanceId=" + processInstanceId;
    }

    @GetMapping("/mostrar-entrada-producto")
    public String mostrarEntradaProducto(@RequestParam("taskId") String taskId, @RequestParam("processInstanceId") String processInstanceId, Model model) {
        model.addAttribute("taskId", taskId);
        model.addAttribute("processInstanceId", processInstanceId);
        // Obtener variables de Camunda
        String urlVars = camundaUrl + "/process-instance/" + processInstanceId + "/variables";
        HttpHeaders headers = createHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Map> responseVars = restTemplate.exchange(urlVars, HttpMethod.GET, request, Map.class);
        Map variables = responseVars.getBody();
        String producto = "";
        String cantidadAComprar = "";
        if (variables != null) {
            Object productoObj = variables.get("productoOrdenado");
            if (productoObj == null) {
                productoObj = variables.get("producto");
            }
            Object cantidadAComprarObj = variables.get("cantidadAComprar");
            producto = productoObj instanceof Map ? String.valueOf(((Map)productoObj).get("value")) : "";
            cantidadAComprar = cantidadAComprarObj instanceof Map ? String.valueOf(((Map)cantidadAComprarObj).get("value")) : "";
        }
        model.addAttribute("producto", producto);
        model.addAttribute("cantidadAComprar", cantidadAComprar);
        return "registrar_entrada_producto";
    }
} 