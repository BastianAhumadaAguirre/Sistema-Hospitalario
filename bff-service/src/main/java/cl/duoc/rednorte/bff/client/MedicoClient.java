package cl.duoc.rednorte.bff.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MedicoClient {

    private final RestTemplate restTemplate;

    @Value("${services.medico.url}")
    private String baseUrl;

    @CircuitBreaker(name = "medicoClient", fallbackMethod = "nombreMedicoFallback")
    public String getNombreMedico(Long usuarioId) {
        Map<String, Object> data = restTemplate.exchange(
            baseUrl + "/api/internal/medicos/usuario/" + usuarioId,
            HttpMethod.GET, null,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();

        if (data == null) return null;
        return ((String) data.get("nombre") + " " + (String) data.get("apellido")).trim();
    }

    String nombreMedicoFallback(Long usuarioId, Throwable t) {
        log.warn("Circuit breaker: getNombreMedico({}) -> {}", usuarioId, t.getMessage());
        return null;
    }
}
