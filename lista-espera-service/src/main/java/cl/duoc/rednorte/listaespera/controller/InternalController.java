package cl.duoc.rednorte.listaespera.controller;

import cl.duoc.rednorte.listaespera.model.CitaMedica;
import cl.duoc.rednorte.listaespera.model.ListaEspera;
import cl.duoc.rednorte.listaespera.model.ListaEspera.EstadoSolicitud;
import cl.duoc.rednorte.listaespera.repository.CitaMedicaRepository;
import cl.duoc.rednorte.listaespera.repository.ListaEsperaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoints internos sin autenticación para uso exclusivo entre microservicios.
 * No deben exponerse hacia internet (usar un API Gateway o firewall de red).
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final ListaEsperaRepository listaEsperaRepo;
    private final CitaMedicaRepository  citaRepo;

    @GetMapping("/lista-espera/paciente/{pacienteId}")
    public List<ListaEspera> getSolicitudesPaciente(@PathVariable Long pacienteId) {
        return listaEsperaRepo.findByPacienteId(pacienteId);
    }

    @GetMapping("/lista-espera/pendientes")
    public List<ListaEspera> getPendientesOrdenados() {
        return listaEsperaRepo.findPendientesOrdenados();
    }

    @GetMapping("/lista-espera/pendientes/count")
    public long countPendientes() {
        return listaEsperaRepo.findByEstado(EstadoSolicitud.PENDIENTE).size();
    }

    @GetMapping("/citas/paciente/{pacienteId}")
    public List<CitaMedica> getCitasPaciente(@PathVariable Long pacienteId) {
        return citaRepo.findByPacienteId(pacienteId);
    }

    /**
     * Citas del día, opcionalmente filtradas por nombre del médico.
     * Usado por el bff-service para el dashboard del médico.
     */
    @GetMapping("/citas/hoy")
    public List<CitaMedica> getCitasHoy(
            @RequestParam(required = false) String nombreMedico,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        LocalDate dia        = fecha != null ? fecha : LocalDate.now();
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fin    = inicio.plusDays(1).minusNanos(1);

        if (nombreMedico != null && !nombreMedico.isBlank()) {
            return citaRepo.findByNombreMedicoAndFechaHoraCitaBetween(nombreMedico, inicio, fin);
        }
        return citaRepo.findByFechaHoraCitaBetweenOrderByFechaHoraCitaAsc(inicio, fin);
    }
}
