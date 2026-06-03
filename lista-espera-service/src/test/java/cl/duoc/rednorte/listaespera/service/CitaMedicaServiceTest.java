package cl.duoc.rednorte.listaespera.service;

import cl.duoc.rednorte.listaespera.dto.CitaMedicaDTO;
import cl.duoc.rednorte.listaespera.model.CitaMedica;
import cl.duoc.rednorte.listaespera.model.CitaMedica.EstadoCita;
import cl.duoc.rednorte.listaespera.model.ListaEspera;
import cl.duoc.rednorte.listaespera.model.ListaEspera.EstadoSolicitud;
import cl.duoc.rednorte.listaespera.repository.CitaMedicaRepository;
import cl.duoc.rednorte.listaespera.repository.ListaEsperaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CitaMedicaService - Pruebas unitarias")
class CitaMedicaServiceTest {

    @Mock private CitaMedicaRepository citaRepo;
    @Mock private ListaEsperaRepository listaEsperaRepo;
    @InjectMocks private CitaMedicaService service;

    private ListaEspera solicitudPendiente;
    private ListaEspera solicitudAsignada;
    private CitaMedica citaProgramada;
    private CitaMedica citaConfirmada;
    private CitaMedica citaRealizada;
    private CitaMedicaDTO dto;

    @BeforeEach
    void setUp() {
        solicitudPendiente = ListaEspera.builder()
                .id(1L).pacienteId(10L).especialidad("Cardiologia")
                .hospital("Hospital Norte").prioridad(1)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now()).build();

        solicitudAsignada = ListaEspera.builder()
                .id(2L).pacienteId(20L).especialidad("Neurologia")
                .hospital("Hospital Central").prioridad(2)
                .estado(EstadoSolicitud.ASIGNADO)
                .fechaSolicitud(LocalDateTime.now()).build();

        citaProgramada = CitaMedica.builder()
                .id(100L).listaEspera(solicitudPendiente)
                .nombreMedico("Dr. Garcia").especialidad("Cardiologia")
                .hospital("Hospital Norte").boxNumero("Box 3")
                .fechaHoraCita(LocalDateTime.now().plusDays(3))
                .estado(EstadoCita.PROGRAMADA).build();

        citaConfirmada = CitaMedica.builder()
                .id(101L).listaEspera(solicitudAsignada)
                .nombreMedico("Dr. Lopez").especialidad("Neurologia")
                .hospital("Hospital Central").boxNumero("Sala 12")
                .fechaHoraCita(LocalDateTime.now().plusDays(5))
                .estado(EstadoCita.CONFIRMADA).build();

        citaRealizada = CitaMedica.builder()
                .id(102L).listaEspera(solicitudAsignada)
                .nombreMedico("Dr. Perez").especialidad("Traumatologia")
                .hospital("Hospital Sur").boxNumero("Box 1")
                .fechaHoraCita(LocalDateTime.now().minusDays(1))
                .estado(EstadoCita.REALIZADA).build();

        dto = CitaMedicaDTO.builder()
                .listaEsperaId(1L).nombreMedico("Dr. Garcia")
                .especialidad("Cardiologia").hospital("Hospital Norte")
                .boxNumero("Box 3").fechaHoraCita(LocalDateTime.now().plusDays(3))
                .observaciones("Control anual").build();
    }

    // ── Flujo de agendamiento (RF2) ──────────────────────────────────────

    @Nested
    @DisplayName("Flujo de agendamiento de cita")
    class AgendarCita {

        @Test
        @DisplayName("Agendar cita como medico → solicitud cambia a ASIGNADO")
        void agendar_exitoso_solicitudCambiaAAsignado() {
            when(listaEsperaRepo.findById(1L)).thenReturn(Optional.of(solicitudPendiente));
            when(citaRepo.existsByListaEsperaId(1L)).thenReturn(false);
            when(citaRepo.save(any())).thenReturn(citaProgramada);
            when(listaEsperaRepo.save(any())).thenReturn(solicitudAsignada);

            CitaMedica resultado = service.agendar(dto);

            assertThat(resultado).isNotNull();
            assertThat(solicitudPendiente.getEstado()).isEqualTo(EstadoSolicitud.ASIGNADO);
            verify(citaRepo).save(any());
            verify(listaEsperaRepo).save(solicitudPendiente);
        }

        @Test
        @DisplayName("Agendar cita → solicitud inexistente lanza excepcion")
        void agendar_solicitudNoExiste_lanzaExcepcion() {
            CitaMedicaDTO dtoInvalido = CitaMedicaDTO.builder().listaEsperaId(99L)
                    .nombreMedico("Dr. X").especialidad("Esp").hospital("H")
                    .boxNumero("B1").fechaHoraCita(LocalDateTime.now().plusDays(1)).build();

            when(listaEsperaRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.agendar(dtoInvalido))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("Agendar cita → solicitud no esta PENDIENTE lanza excepcion")
        void agendar_solicitudNoEsPendiente_lanzaExcepcion() {
            CitaMedicaDTO dtoAsignado = CitaMedicaDTO.builder().listaEsperaId(1L)
                    .nombreMedico("Dr. X").especialidad("Esp").hospital("H")
                    .boxNumero("B1").fechaHoraCita(LocalDateTime.now().plusDays(1)).build();

            when(listaEsperaRepo.findById(1L)).thenReturn(Optional.of(solicitudAsignada));

            assertThatThrownBy(() -> service.agendar(dtoAsignado))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PENDIENTES");
        }

        @Test
        @DisplayName("Agendar cita → solicitud ya tiene cita asignada lanza excepcion")
        void agendar_citaYaExiste_lanzaExcepcion() {
            when(listaEsperaRepo.findById(1L)).thenReturn(Optional.of(solicitudPendiente));
            when(citaRepo.existsByListaEsperaId(1L)).thenReturn(true);

            assertThatThrownBy(() -> service.agendar(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ya tiene una cita");
        }
    }

    // ── Flujo de cancelacion con reversion de estado (RF3) ───────────────

    @Nested
    @DisplayName("Flujo de cancelacion de cita con motivo")
    class CancelarCita {

        @Test
        @DisplayName("Cancelar cita con motivo → CANCELADA y solicitud vuelve a PENDIENTE")
        void cancelar_registraMotivoYrevierteSolicitud() {
            when(citaRepo.findById(100L)).thenReturn(Optional.of(citaProgramada));
            when(citaRepo.save(any())).thenReturn(citaProgramada);
            when(listaEsperaRepo.save(any())).thenReturn(solicitudPendiente);

            CitaMedica resultado = service.cancelar(100L, "Medico no disponible");

            assertThat(resultado.getEstado()).isEqualTo(EstadoCita.CANCELADA);
            assertThat(citaProgramada.getListaEspera().getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
            verify(listaEsperaRepo).save(solicitudPendiente);
        }

        @Test
        @DisplayName("Cancelar cita CONFIRMADA → estado CANCELADA")
        void cancelar_confirmadaACancelada() {
            when(citaRepo.findById(101L)).thenReturn(Optional.of(citaConfirmada));
            when(citaRepo.save(any())).thenReturn(citaConfirmada);
            when(listaEsperaRepo.save(any())).thenReturn(solicitudAsignada);

            CitaMedica resultado = service.cancelar(101L, "Paciente solicito cancelar");

            assertThat(resultado.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        }

        @Test
        @DisplayName("Cancelar cita ya REALIZADA → lanza excepcion")
        void cancelar_yaRealizada_lanzaExcepcion() {
            when(citaRepo.findById(102L)).thenReturn(Optional.of(citaRealizada));

            assertThatThrownBy(() -> service.cancelar(102L, "motivo"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("cancelar");
        }
    }

    // ── Consultas de citas ───────────────────────────────────────────────

    @Nested
    @DisplayName("Consultas de citas")
    class Consultas {

        @Test
        @DisplayName("Obtener cita por ID existente")
        void obtenerPorId_existente_retornaCita() {
            when(citaRepo.findById(100L)).thenReturn(Optional.of(citaProgramada));

            CitaMedica resultado = service.obtenerPorId(100L);

            assertThat(resultado.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Obtener cita por ID inexistente → lanza excepcion")
        void obtenerPorId_noExistente_lanzaExcepcion() {
            when(citaRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerPorId(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("999");
        }
    }
}
