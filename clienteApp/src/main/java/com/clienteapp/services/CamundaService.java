package com.clienteapp.services;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;

@Service
public class CamundaService {
    private static final Logger logger = LoggerFactory.getLogger(CamundaService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final String camundaUrl = "http://localhost:8080/engine-rest";
    private final String username = "admin";
    private final String password = "123";

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }

    // Obtener tareas asignadas a un usuario
    public Task[] getTasksByAssignee(String assignee) {
        try {
            String url = camundaUrl + "/task?assignee=" + assignee;
            logger.info("Consultando tareas para assignee: {} en URL: {}", assignee, url);
            HttpHeaders headers = createHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<Task[]> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                request, 
                Task[].class
            );
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error al obtener tareas para assignee {}: {}", assignee, e.getMessage());
            throw new RuntimeException("Error al comunicarse con Camunda: " + e.getMessage());
        }
    }

    // Completar tarea
    public void completeTask(String taskId) {
        try {
            String url = camundaUrl + "/task/" + taskId + "/complete";
            logger.info("Completando tarea {} en URL: {}", taskId, url);
            HttpHeaders headers = createHeaders();
            HttpEntity<?> request = new HttpEntity<>(null, headers);
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
        } catch (RestClientException e) {
            logger.error("Error al completar tarea {}: {}", taskId, e.getMessage());
            throw new RuntimeException("Error al comunicarse con Camunda: " + e.getMessage());
        }
    }

    // Completar tarea con variables
    public void completeTaskWithVariables(String taskId, Map<String, Object> variables) {
        try {
            String url = camundaUrl + "/task/" + taskId + "/complete";
            Map<String, Object> body = new HashMap<>();
            Map<String, Object> vars = new HashMap<>();

            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                Map<String, Object> varInfo = new HashMap<>();
                Object val = entry.getValue();
                if (val instanceof String) {
                    varInfo.put("value", val);
                    varInfo.put("type", "String");
                } else if (val instanceof Integer) {
                    varInfo.put("value", val);
                    varInfo.put("type", "Integer");
                } else if (val instanceof Double) {
                    varInfo.put("value", val);
                    varInfo.put("type", "Double");
                } else {
                    varInfo.put("value", val);
                    varInfo.put("type", "Object");
                }
                vars.put(entry.getKey(), varInfo);
            }
            body.put("variables", vars);

            logger.info("Completando tarea {} con variables: {}", taskId, body);
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
        } catch (RestClientException e) {
            logger.error("Error al completar tarea {} con variables: {}", taskId, e.getMessage());
            throw new RuntimeException("Error al comunicarse con Camunda: " + e.getMessage());
        }
    }

    // Nueva clase para mapear la respuesta del startForm
    public static class FormResponse {
        private String key;
        private String contextPath;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getContextPath() { return contextPath; }
        public void setContextPath(String contextPath) { this.contextPath = contextPath; }
    }

    // Nuevo método para obtener el formKey del evento de inicio
    public String getStartFormKey(String processKey) {
        try {
            String url = camundaUrl + "/process-definition/key/" + processKey + "/startForm";
            logger.info("Obteniendo formKey para proceso {} en URL: {}", processKey, url);
            HttpHeaders headers = createHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<FormResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                request, 
                FormResponse.class
            );
            FormResponse formResponse = response.getBody();
            return formResponse != null ? formResponse.getKey() : null;
        } catch (RestClientException e) {
            logger.error("Error al obtener formKey para proceso {}: {}", processKey, e.getMessage());
            throw new RuntimeException("Error al comunicarse con Camunda: " + e.getMessage());
        }
    }

    // Modela clase Task según el JSON de Camunda
    public static class Task {
        private String id;
        private String name;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public String startProcessWithVariables(String processKey, Map<String, Object> variables) {
        try {
            String url = camundaUrl + "/process-definition/key/" + processKey + "/start";
            Map<String, Object> body = new HashMap<>();
            Map<String, Object> vars = new HashMap<>();

            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                Map<String, Object> varInfo = new HashMap<>();
                Object val = entry.getValue();

                if (entry.getKey().equals("fechaCompra")) {
                    Date fecha = null;
                    if (val instanceof Date) {
                        fecha = (Date) val;
                    } else if (val instanceof String) {
                        try {
                            fecha = new SimpleDateFormat("yyyy-MM-dd").parse((String) val);
                        } catch (Exception e) {
                            throw new RuntimeException("Formato de fechaCompra inválido: " + val);
                        }
                    }
                    if (fecha != null) {
                        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        String isoDate = isoFormat.format(fecha);
                        varInfo.put("value", isoDate);
                        varInfo.put("type", "Date");
                    } else {
                        varInfo.put("value", val);
                        varInfo.put("type", "String");
                    }
                } else if (entry.getKey().equals("productos")) {
                    varInfo.put("value", val);
                    varInfo.put("type", "Json");
                } else if (val instanceof String) {
                    varInfo.put("value", val);
                    varInfo.put("type", "String");
                } else if (val instanceof Integer) {
                    varInfo.put("value", val);
                    varInfo.put("type", "Integer");
                } else if (val instanceof Date) {
                    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                    isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String isoDate = isoFormat.format((Date) val);
                    varInfo.put("value", isoDate);
                    varInfo.put("type", "String");
                } else {
                    varInfo.put("value", val);
                    varInfo.put("type", "Object");
                }

                vars.put(entry.getKey(), varInfo);
            }
            body.put("variables", vars);

            logger.info("Iniciando proceso {} con variables: {}", processKey, body);
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                Map.class
            );
            if (response.getStatusCode() != HttpStatus.OK && response.getStatusCode() != HttpStatus.CREATED) {
                throw new RuntimeException("Error al iniciar proceso: " + response.getStatusCode());
            }
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("id")) {
                return responseBody.get("id").toString();
            } else {
                throw new RuntimeException("No se pudo obtener el ID de la instancia creada");
            }
        } catch (RestClientException e) {
            logger.error("Error al iniciar proceso {}: {}", processKey, e.getMessage());
            throw new RuntimeException("Error al comunicarse con Camunda: " + e.getMessage());
        }
    }

    public void enviarMensajeAprobacionSupervisor(String processInstanceId) {
        String url = camundaUrl + "/message";
        HttpHeaders headers = createHeaders();
        Map<String, Object> body = new HashMap<>();
        body.put("messageName", "mensaje_aprobacion_supervisor");
        body.put("processInstanceId", processInstanceId);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(url, request, String.class);
            logger.info("Mensaje de aprobación enviado a la instancia {}", processInstanceId);
        } catch (RestClientException e) {
            logger.error("Error al enviar mensaje de aprobación: {}", e.getMessage());
            throw new RuntimeException("Error al enviar mensaje de aprobación: " + e.getMessage());
        }
    }

    public void enviarMensajeReponerStock(String processInstanceId) {
        String url = camundaUrl + "/message";
        HttpHeaders headers = createHeaders();
        Map<String, Object> body = new HashMap<>();
        body.put("messageName", "ReponerStock");
        body.put("processInstanceId", processInstanceId);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(url, request, String.class);
            logger.info("Mensaje 'ReponerStock' enviado a la instancia {}", processInstanceId);
        } catch (RestClientException e) {
            logger.error("Error al enviar mensaje 'ReponerStock': {}", e.getMessage());
            throw new RuntimeException("Error al enviar mensaje 'ReponerStock': " + e.getMessage());
        }
    }

    public Task[] getTasksByCandidateGroupAndInstance(String candidateGroup, String processInstanceId) {
        try {
            String url = camundaUrl + "/task?candidateGroup=" + candidateGroup + "&processInstanceId=" + processInstanceId;
            logger.info("Consultando tareas para candidateGroup: {} y processInstanceId: {} en URL: {}", candidateGroup, processInstanceId, url);
            HttpHeaders headers = createHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<Task[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                Task[].class
            );
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error al obtener tareas para candidateGroup {} y processInstanceId {}: {}", candidateGroup, processInstanceId, e.getMessage());
            throw new RuntimeException("Error al comunicarse con Camunda: " + e.getMessage());
        }
    }

    public Map<String, Object> getProcessVariables(String processInstanceId) {
        try {
            String url = camundaUrl + "/process-instance/" + processInstanceId + "/variables";
            logger.info("Consultando variables para processInstanceId: {} en URL: {}", processInstanceId, url);
            HttpHeaders headers = createHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                Map.class
            );
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error al obtener variables para processInstanceId {}: {}", processInstanceId, e.getMessage());
            throw new RuntimeException("Error al comunicarse con Camunda: " + e.getMessage());
        }
    }
}
