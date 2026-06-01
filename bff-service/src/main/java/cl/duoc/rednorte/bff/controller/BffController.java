package cl.duoc.rednorte.bff.controller;

import cl.duoc.rednorte.bff.client.ListaEsperaClient;
import cl.duoc.rednorte.bff.client.MedicoClient;
import cl.duoc.rednorte.bff.client.PacienteClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bff")
@RequiredArgsConstructor
public class BffController {

    private final ListaEsperaClient listaEsperaClient;
    private final MedicoClient      medicoClient;
    private final PacienteClient    pacienteClient;

    // ── Dashboard Paciente ────────────────────────────────────────────
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<Map<String, Object>> dashboardPaciente(@PathVariable Long pacienteId) {

        List<Map<String, Object>> solicitudes      = listaEsperaClient.getSolicitudesPaciente(pacienteId);
        List<Map<String, Object>> citas            = listaEsperaClient.getCitasPaciente(pacienteId);
        List<Map<String, Object>> pendientesOrden  = listaEsperaClient.getPendientesOrdenados();

        long solicitudesActivas = solicitudes.stream()
            .filter(s -> "PENDIENTE".equals(s.get("estado")) || "ASIGNADO".equals(s.get("estado")))
            .count();

        // Enriquecer solicitudes PENDIENTE con posición en la fila
        List<List<Object>> pendientesIds = pendientesOrden.stream()
            .map(p -> List.of(p.get("id")))
            .collect(Collectors.toList());

        List<Map<String, Object>> solicitudesDTO = solicitudes.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>(s);
            if ("PENDIENTE".equals(s.get("estado"))) {
                int pos = indexOfId(pendientesOrden, s.get("id")) + 1;
                m.put("posicion",              pos > 0 ? pos : null);
                m.put("tiempoEstimadoMinutos", pos > 0 ? pos * 20 : null);
            } else {
                m.put("posicion",              null);
                m.put("tiempoEstimadoMinutos", null);
            }
            return m;
        }).collect(Collectors.toList());

        // Próxima cita activa futura
        Map<String, Object> proximaCita = citas.stream()
            .filter(c -> "PROGRAMADA".equals(c.get("estado")) || "CONFIRMADA".equals(c.get("estado")))
            .filter(c -> {
                Object fh = c.get("fechaHoraCita");
                if (fh == null) return false;
                return LocalDateTime.parse(fh.toString()).isAfter(LocalDateTime.now());
            })
            .min(Comparator.comparing(c -> LocalDateTime.parse(c.get("fechaHoraCita").toString())))
            .orElse(null);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("solicitudes",        solicitudesDTO);
        response.put("citas",              citas);
        response.put("solicitudesActivas", solicitudesActivas);
        response.put("proximaCita",        proximaCita);

        return ResponseEntity.ok(response);
    }

    // ── Dashboard Médico ──────────────────────────────────────────────
    @GetMapping("/medico/{medicoUsuarioId}")
    public ResponseEntity<Map<String, Object>> dashboardMedico(@PathVariable Long medicoUsuarioId) {

        String nombreMedico = medicoClient.getNombreMedico(medicoUsuarioId);

        List<Map<String, Object>> citasHoy = listaEsperaClient.getCitasHoy(nombreMedico, null);

        long totalPendientes     = listaEsperaClient.countPendientes();
        long atendidosHoy        = citasHoy.stream().filter(c -> "REALIZADA".equals(c.get("estado"))).count();
        long citasProgramadasHoy = citasHoy.stream()
            .filter(c -> "PROGRAMADA".equals(c.get("estado")) || "CONFIRMADA".equals(c.get("estado")))
            .count();

        // Enriquecer citas con nombre del paciente
        Set<Long> pacienteIds = citasHoy.stream()
            .map(c -> extractPacienteId(c))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, String> nombresPacientes = pacienteClient.getNombresPacientes(pacienteIds);

        List<Map<String, Object>> citasHoyDTO = citasHoy.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>(c);
            Long pacienteId = extractPacienteId(c);
            if (pacienteId != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> le = (Map<String, Object>) m.get("listaEspera");
                if (le != null) {
                    Map<String, Object> leEnriquecido = new LinkedHashMap<>(le);
                    leEnriquecido.put("pacienteNombre",
                        nombresPacientes.getOrDefault(pacienteId, "Paciente #" + pacienteId));
                    m.put("listaEspera", leEnriquecido);
                }
            }
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("citasHoy",            citasHoyDTO);
        response.put("totalPendientes",      totalPendientes);
        response.put("atendidosHoy",         atendidosHoy);
        response.put("citasProgramadasHoy",  citasProgramadasHoy);

        return ResponseEntity.ok(response);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private int indexOfId(List<Map<String, Object>> list, Object targetId) {
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).get("id"), targetId)) return i;
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private Long extractPacienteId(Map<String, Object> cita) {
        Object le = cita.get("listaEspera");
        if (le instanceof Map) {
            Object pid = ((Map<String, Object>) le).get("pacienteId");
            if (pid instanceof Number) return ((Number) pid).longValue();
        }
        return null;
    }
}
