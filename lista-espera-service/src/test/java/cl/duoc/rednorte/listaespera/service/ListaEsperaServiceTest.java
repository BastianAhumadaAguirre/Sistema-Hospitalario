package cl.duoc.rednorte.listaespera.service;

import cl.duoc.rednorte.listaespera.dto.ListaEsperaDTO;
import cl.duoc.rednorte.listaespera.model.ListaEspera;
import cl.duoc.rednorte.listaespera.model.ListaEspera.EstadoSolicitud;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListaEsperaService - Pruebas unitarias")
class ListaEsperaServiceTest {

    @Mock
    private ListaEsperaRepository repo;

    @InjectMocks
    private ListaEsperaService service;

    private ListaEspera solicitudPendiente;
    private ListaEspera solicitudAsignada;
    private ListaEspera solicitudAtendida;

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
                .fechaSolicitud(LocalDateTime.now())
                .fechaAtencion(LocalDateTime.now().plusDays(1)).build();

        solicitudAtendida = ListaEspera.builder()
                .id(3L).pacienteId(30L).especialidad("Traumatologia")
                .hospital("Hospital Sur").prioridad(3)
                .estado(EstadoSolicitud.ATENDIDO)
                .fechaSolicitud(LocalDateTime.now().minusDays(5)).build();
    }

    // ── RF1: Registro de solicitud ───────────────────────────────────────

    @Nested
    @DisplayName("RF1 - Registrar solicitud en lista de espera")
    class RegistrarSolicitud {

        @Test
        @DisplayName("Crear solicitud como paciente → estado PENDIENTE")
        void registrar_creaConEstadoPendiente() {
            ListaEsperaDTO dto = ListaEsperaDTO.builder()
                    .pacienteId(10L).especialidad("Cardiologia")
                    .hospital("Hospital Norte").prioridad(1)
                    .observaciones("Urgente").build();

            when(repo.save(any())).thenReturn(solicitudPendiente);

            ListaEspera resultado = service.registrar(dto);

            assertThat(resultado).isNotNull();
            verify(repo).save(any(ListaEspera.class));
        }

        @Test
        @DisplayName("Solicitud registrada queda visible en lista del medico")
        void registrar_solicitudVisibleParaMedico() {
            ListaEsperaDTO dto = ListaEsperaDTO.builder()
                    .pacienteId(5L).especialidad("Neurologia")
                    .hospital("Hospital Central").prioridad(2).build();

            ListaEspera guardada = ListaEspera.builder()
                    .id(99L).pacienteId(5L).especialidad("Neurologia")
                    .hospital("Hospital Central").prioridad(2)
                    .estado(EstadoSolicitud.PENDIENTE)
                    .fechaSolicitud(LocalDateTime.now()).build();

            when(repo.save(any())).thenReturn(guardada);

            ListaEspera resultado = service.registrar(dto);

            assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
            assertThat(resultado.getId()).isEqualTo(99L);
        }
    }

    // ── RF2: Asignacion de cupo ──────────────────────────────────────────

    @Nested
    @DisplayName("RF2 - Asignar cupo a solicitud pendiente")
    class AsignarCupo {

        @Test
        @DisplayName("Asignar solicitud PENDIENTE → cambia a ASIGNADO")
        void asignar_pendienteAAsignado() {
            when(repo.findById(1L)).thenReturn(Optional.of(solicitudPendiente));
            when(repo.save(any())).thenReturn(solicitudPendiente);

            ListaEspera resultado = service.asignar(1L);

            assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ASIGNADO);
            verify(repo).save(solicitudPendiente);
        }

        @Test
        @DisplayName("Asignar solicitud no PENDIENTE → lanza excepcion")
        void asignar_noEsPendiente_lanzaExcepcion() {
            when(repo.findById(2L)).thenReturn(Optional.of(solicitudAsignada));

            assertThatThrownBy(() -> service.asignar(2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PENDIENTES");
        }
    }

    // ── RF3: Cancelacion ────────────────────────────────────────────────

    @Nested
    @DisplayName("RF3 - Cancelar solicitud")
    class CancelarSolicitud {

        @Test
        @DisplayName("Cancelar solicitud PENDIENTE → estado CANCELADO")
        void cancelar_pendienteACancelado() {
            when(repo.findById(1L)).thenReturn(Optional.of(solicitudPendiente));
            when(repo.save(any())).thenReturn(solicitudPendiente);

            ListaEspera resultado = service.cancelar(1L);

            assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.CANCELADO);
        }

        @Test
        @DisplayName("Cancelar solicitud ya ATENDIDA → lanza excepcion")
        void cancelar_yaAtendido_lanzaExcepcion() {
            when(repo.findById(3L)).thenReturn(Optional.of(solicitudAtendida));

            assertThatThrownBy(() -> service.cancelar(3L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ATENDIDA");
        }
    }

    // ── Consultas ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Consultas de lista de espera")
    class Consultas {

        @Test
        @DisplayName("Obtener solicitudes pendientes ordenadas por prioridad")
        void obtenerPendientes_retornaListaOrdenada() {
            when(repo.findPendientesOrdenados()).thenReturn(List.of(solicitudPendiente));

            List<ListaEspera> resultado = service.obtenerPendientes();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
            verify(repo).findPendientesOrdenados();
        }

        @Test
        @DisplayName("Obtener solicitud por ID existente")
        void obtenerPorId_existente_retornaSolicitud() {
            when(repo.findById(1L)).thenReturn(Optional.of(solicitudPendiente));

            ListaEspera resultado = service.obtenerPorId(1L);

            assertThat(resultado.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Obtener solicitud por ID inexistente → lanza excepcion")
        void obtenerPorId_noExistente_lanzaExcepcion() {
            when(repo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerPorId(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }
    }
}
