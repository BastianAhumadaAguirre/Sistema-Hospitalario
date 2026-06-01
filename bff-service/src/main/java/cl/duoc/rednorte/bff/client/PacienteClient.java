package cl.duoc.rednorte.bff.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PacienteClient {

    private final RestTemplate restTemplate;

    @Value("${services.paciente.url}")
    private String baseUrl;

    @CircuitBreaker(name = "pacienteClient", fallbackMethod = "nombresPacientesFallback")
    public Map<Long, String> getNombresPacientes(Set<Long> ids) {
        Map<Long, String> result = new HashMap<>();
        for (Long id : ids) {
            try {
                Map<String, Object> data = restTemplate.exchange(
                    baseUrl + "/api/internal/pacientes/" + id,
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                ).getBody();
                if (data != null) {
                    result.put(id, ((String) data.get("nombre") + " " + (String) data.get("apellido")).trim());
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener nombre del paciente {}: {}", id, e.getMessage());
            }
        }
        return result;
    }

    Map<Long, String> nombresPacientesFallback(Set<Long> ids, Throwable t) {
        log.warn("Circuit breaker: getNombresPacientes -> {}", t.getMessage());
        return new HashMap<>();
    }
}
