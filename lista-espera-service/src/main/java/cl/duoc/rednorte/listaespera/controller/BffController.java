package cl.duoc.rednorte.listaespera.controller;

import cl.duoc.rednorte.listaespera.model.CitaMedica;
import cl.duoc.rednorte.listaespera.model.ListaEspera;
import cl.duoc.rednorte.listaespera.model.ListaEspera.EstadoSolicitud;
import cl.duoc.rednorte.listaespera.repository.CitaMedicaRepository;
import cl.duoc.rednorte.listaespera.repository.ListaEsperaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bff")
@RequiredArgsConstructor
@Slf4j
public class BffController {

    private final ListaEsperaRepository listaEsperaRepo;
    private final CitaMedicaRepository  citaRepo;
    private final RestTemplate          restTemplate;

    @Value("${services.paciente.url}")
    private String pacienteServiceUrl;

    @Value("${services.medico.url}")
    private String medicoServiceUrl;

    // ── Dashboard Paciente ────────────────────────────────────────────
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<Map<String, Object>> dashboardPaciente(@PathVariable Long pacienteId) {

        List<ListaEspera> solicitudes = listaEsperaRepo.findByPacienteId(pacienteId);
        List<CitaMedica>  citas       = citaRepo.findByPacienteId(pacienteId);

        long solicitudesActivas = solicitudes.stream()
            .filter(s -> s.getEstado() == EstadoSolicitud.PENDIENTE
                      || s.getEstado() == EstadoSolicitud.ASIGNADO)
            .count();

        Optional<CitaMedica> proxima = citas.stream()
            .filter(c -> c.getEstado() == CitaMedica.EstadoCita.PROGRAMADA
                      || c.getEstado() == CitaMedica.EstadoCita.CONFIRMADA)
            .filter(c -> c.getFechaHoraCita().isAfter(LocalDateTime.now()))
            .min(Comparator.comparing(CitaMedica::getFechaHoraCita));

        List<ListaEspera> pendientesOrdenados = listaEsperaRepo.findPendientesOrdenados();

        List<Map<String, Object>> solicitudesDTO = solicitudes.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",           s.getId());
            m.put("especialidad", s.getEspecialidad());
            m.put("hospital",     s.getHospital());
            m.put("estado",       s.getEstado().name());
            m.put("fechaSolicitud", s.getFechaSolicitud());
            m.put("observaciones",  s.getObservaciones());
            if (s.getEstado() == EstadoSolicitud.PENDIENTE) {
                int pos = pendientesOrdenados.indexOf(s) + 1;
                m.put("posicion",              pos > 0 ? pos : null);
                m.put("tiempoEstimadoMinutos", pos > 0 ? pos * 20 : null);
            } else {
                m.put("posicion",              null);
                m.put("tiempoEstimadoMinutos", null);
            }
            return m;
        }).collect(Collectors.toList());

        List<Map<String, Object>> citasDTO = citas.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",          c.getId());
            m.put("estado",      c.getEstado().name());
            m.put("especialidad",c.getEspecialidad());
            m.put("nombreMedico",c.getNombreMedico());
            m.put("fechaHoraCita", c.getFechaHoraCita());
            m.put("hospital",    c.getHospital());
            m.put("boxNumero",   c.getBoxNumero());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> proximaCitaDTO = proxima.map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",           c.getId());
            m.put("especialidad", c.getEspecialidad());
            m.put("nombreMedico", c.getNombreMedico());
            m.put("fechaHoraCita",c.getFechaHoraCita());
            m.put("hospital",     c.getHospital());
            return m;
        }).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("solicitudes",       solicitudesDTO);
        response.put("citas",             citasDTO);
        response.put("solicitudesActivas",solicitudesActivas);
        response.put("proximaCita",       proximaCitaDTO);

        return ResponseEntity.ok(response);
    }

    // ── Dashboard Médico ──────────────────────────────────────────────
    @GetMapping("/medico/{medicoUsuarioId}")
    public ResponseEntity<Map<String, Object>> dashboardMedico(@PathVariable Long medicoUsuarioId) {

        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia    = inicioDia.plusDays(1).minusNanos(1);

        String nombreMedico = fetchNombreMedico(medicoUsuarioId);
        List<CitaMedica> citasHoy;
        if (nombreMedico != null) {
            citasHoy = citaRepo.findByNombreMedicoAndFechaHoraCitaBetween(nombreMedico, inicioDia, finDia);
        } else {
            citasHoy = citaRepo.findByFechaHoraCitaBetweenOrderByFechaHoraCitaAsc(inicioDia, finDia);
        }

        long totalPendientes     = listaEsperaRepo.findByEstado(EstadoSolicitud.PENDIENTE).size();
        long atendidosHoy        = citasHoy.stream().filter(c -> c.getEstado() == CitaMedica.EstadoCita.REALIZADA).count();
        long citasProgramadasHoy = citasHoy.stream()
            .filter(c -> c.getEstado() == CitaMedica.EstadoCita.PROGRAMADA
                      || c.getEstado() == CitaMedica.EstadoCita.CONFIRMADA).count();

        Set<Long> pacienteIds = citasHoy.stream()
            .map(c -> c.getListaEspera().getPacienteId())
            .collect(Collectors.toSet());

        Map<Long, String> nombresPacientes = fetchNombresPacientes(pacienteIds);

        List<Map<String, Object>> citasHoyDTO = citasHoy.stream().map(c -> {
            Long pacienteId  = c.getListaEspera().getPacienteId();
            String pacNombre = nombresPacientes.getOrDefault(pacienteId, "Paciente #" + pacienteId);

            Map<String, Object> listaEsperaMap = new HashMap<>();
            listaEsperaMap.put("id",             c.getListaEspera().getId());
            listaEsperaMap.put("pacienteId",     pacienteId);
            listaEsperaMap.put("pacienteNombre", pacNombre);
            listaEsperaMap.put("especialidad",   c.getListaEspera().getEspecialidad());

            Map<String, Object> m = new HashMap<>();
            m.put("id",           c.getId());
            m.put("estado",       c.getEstado().name());
            m.put("especialidad", c.getEspecialidad());
            m.put("nombreMedico", c.getNombreMedico());
            m.put("fechaHoraCita",c.getFechaHoraCita());
            m.put("boxNumero",    c.getBoxNumero());
            m.put("hospital",     c.getHospital());
            m.put("listaEspera",  listaEsperaMap);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("citasHoy",            citasHoyDTO);
        response.put("totalPendientes",      totalPendientes);
        response.put("atendidosHoy",         atendidosHoy);
        response.put("citasProgramadasHoy",  citasProgramadasHoy);

        return ResponseEntity.ok(response);
    }

    // ── Helpers con Circuit Breaker ───────────────────────────────────

    @CircuitBreaker(name = "medicoClient", fallbackMethod = "fetchNombreMedicoFallback")
    String fetchNombreMedico(Long usuarioId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = restTemplate.getForObject(
            medicoServiceUrl + "/api/internal/medicos/usuario/" + usuarioId, Map.class);
        if (data == null) return null;
        return ((String) data.get("nombre") + " " + (String) data.get("apellido")).trim();
    }

    String fetchNombreMedicoFallback(Long usuarioId, Throwable t) {
        log.warn("Circuit breaker activo para medico-service (usuarioId={}): {}", usuarioId, t.getMessage());
        return null;
    }

    @CircuitBreaker(name = "pacienteClient", fallbackMethod = "fetchNombresPacientesFallback")
    Map<Long, String> fetchNombresPacientes(Set<Long> ids) {
        Map<Long, String> result = new HashMap<>();
        for (Long id : ids) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = restTemplate.getForObject(
                    pacienteServiceUrl + "/api/internal/pacientes/" + id, Map.class);
                if (data != null) {
                    result.put(id, ((String) data.get("nombre") + " " + (String) data.get("apellido")).trim());
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener nombre del paciente {}: {}", id, e.getMessage());
            }
        }
        return result;
    }

    Map<Long, String> fetchNombresPacientesFallback(Set<Long> ids, Throwable t) {
        log.warn("Circuit breaker activo para paciente-service: {}", t.getMessage());
        return Collections.emptyMap();
    }
}
