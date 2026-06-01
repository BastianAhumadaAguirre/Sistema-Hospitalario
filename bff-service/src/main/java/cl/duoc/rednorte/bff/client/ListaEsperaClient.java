package cl.duoc.rednorte.bff.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListaEsperaClient {

    private final RestTemplate restTemplate;

    @Value("${services.lista-espera.url}")
    private String baseUrl;

    @CircuitBreaker(name = "listaEsperaClient", fallbackMethod = "solicitudesFallback")
    public List<Map<String, Object>> getSolicitudesPaciente(Long pacienteId) {
        return restTemplate.exchange(
            baseUrl + "/api/internal/lista-espera/paciente/" + pacienteId,
            HttpMethod.GET, null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    List<Map<String, Object>> solicitudesFallback(Long pacienteId, Throwable t) {
        log.warn("Circuit breaker: getSolicitudesPaciente({}) -> {}", pacienteId, t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "listaEsperaClient", fallbackMethod = "pendientesFallback")
    public List<Map<String, Object>> getPendientesOrdenados() {
        return restTemplate.exchange(
            baseUrl + "/api/internal/lista-espera/pendientes",
            HttpMethod.GET, null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    List<Map<String, Object>> pendientesFallback(Throwable t) {
        log.warn("Circuit breaker: getPendientesOrdenados -> {}", t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "listaEsperaClient", fallbackMethod = "countPendientesFallback")
    public long countPendientes() {
        Long count = restTemplate.getForObject(
            baseUrl + "/api/internal/lista-espera/pendientes/count", Long.class);
        return count != null ? count : 0L;
    }

    long countPendientesFallback(Throwable t) {
        log.warn("Circuit breaker: countPendientes -> {}", t.getMessage());
        return 0L;
    }

    @CircuitBreaker(name = "listaEsperaClient", fallbackMethod = "citasPacienteFallback")
    public List<Map<String, Object>> getCitasPaciente(Long pacienteId) {
        return restTemplate.exchange(
            baseUrl + "/api/internal/citas/paciente/" + pacienteId,
            HttpMethod.GET, null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    List<Map<String, Object>> citasPacienteFallback(Long pacienteId, Throwable t) {
        log.warn("Circuit breaker: getCitasPaciente({}) -> {}", pacienteId, t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "listaEsperaClient", fallbackMethod = "citasHoyFallback")
    public List<Map<String, Object>> getCitasHoy(String nombreMedico, LocalDate fecha) {
        StringBuilder url = new StringBuilder(baseUrl + "/api/internal/citas/hoy");
        boolean hasParam = false;
        if (nombreMedico != null && !nombreMedico.isBlank()) {
            url.append("?nombreMedico=").append(nombreMedico);
            hasParam = true;
        }
        if (fecha != null) {
            url.append(hasParam ? "&" : "?").append("fecha=").append(fecha);
        }
        return restTemplate.exchange(
            url.toString(), HttpMethod.GET, null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    List<Map<String, Object>> citasHoyFallback(String nombreMedico, LocalDate fecha, Throwable t) {
        log.warn("Circuit breaker: getCitasHoy -> {}", t.getMessage());
        return Collections.emptyList();
    }
}
