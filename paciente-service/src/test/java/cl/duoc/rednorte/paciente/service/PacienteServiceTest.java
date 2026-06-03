package cl.duoc.rednorte.paciente.service;

import cl.duoc.rednorte.paciente.dto.PacienteDTO;
import cl.duoc.rednorte.paciente.model.Paciente;
import cl.duoc.rednorte.paciente.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PacienteService - Pruebas unitarias")
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteService service;

    private Paciente paciente;
    private PacienteDTO dto;

    @BeforeEach
    void setUp() {
        paciente = Paciente.builder()
                .id(1L).rut("12345678-9").nombre("Juan").apellido("Perez")
                .email("juan.perez@mail.com").telefono("56912345678")
                .direccion("Calle 123").fechaNacimiento(LocalDate.of(1990, 5, 15))
                .build();

        dto = PacienteDTO.builder()
                .rut("12345678-9").nombre("Juan").apellido("Perez")
                .email("juan.perez@mail.com").telefono("56912345678")
                .direccion("Calle 123").fechaNacimiento(LocalDate.of(1990, 5, 15))
                .build();
    }

    // ── Consultas (RF4) ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Consultas de pacientes")
    class Consultas {

        @Test
        @DisplayName("Listar todos los pacientes del sistema")
        void listarTodos_retornaListaCompleta() {
            when(pacienteRepository.findAll()).thenReturn(List.of(paciente));

            List<Paciente> resultado = service.listarTodos();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNombre()).isEqualTo("Juan");
        }

        @Test
        @DisplayName("Obtener paciente por ID existente")
        void obtenerPorId_existente_retornaPaciente() {
            when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

            Paciente resultado = service.obtenerPorId(1L);

            assertThat(resultado.getRut()).isEqualTo("12345678-9");
        }

        @Test
        @DisplayName("Obtener paciente por ID inexistente → lanza excepcion")
        void obtenerPorId_noExistente_lanzaExcepcion() {
            when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerPorId(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── Registro de paciente (RF1) ───────────────────────────────────────

    @Nested
    @DisplayName("Registro de paciente")
    class RegistroPaciente {

        @Test
        @DisplayName("Registrar paciente nuevo → se guarda correctamente")
        void crear_rutNuevo_guardaPaciente() {
            when(pacienteRepository.existsByRut("12345678-9")).thenReturn(false);
            when(pacienteRepository.save(any())).thenReturn(paciente);

            Paciente resultado = service.crear(dto);

            assertThat(resultado.getNombre()).isEqualTo("Juan");
            verify(pacienteRepository).save(any());
        }

        @Test
        @DisplayName("Registrar paciente con RUT duplicado → lanza excepcion")
        void crear_rutDuplicado_lanzaExcepcion() {
            when(pacienteRepository.existsByRut("12345678-9")).thenReturn(true);

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("RUT");

            verify(pacienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Actualizar datos de contacto del paciente")
        void actualizar_existente_actualizaDatos() {
            PacienteDTO dtoActualizado = PacienteDTO.builder()
                    .nombre("Juan Carlos").apellido("Perez Gonzalez")
                    .email("jc.perez@mail.com").telefono("56987654321")
                    .direccion("Avenida Siempre Viva 742").rut("12345678-9")
                    .fechaNacimiento(LocalDate.of(1985, 3, 10)).build();

            when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
            when(pacienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Paciente resultado = service.actualizar(1L, dtoActualizado);

            assertThat(resultado.getNombre()).isEqualTo("Juan Carlos");
            assertThat(resultado.getEmail()).isEqualTo("jc.perez@mail.com");
        }

        @Test
        @DisplayName("Eliminar paciente existente del sistema")
        void eliminar_existente_llamaDeleteById() {
            when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

            service.eliminar(1L);

            verify(pacienteRepository).deleteById(1L);
        }
    }

    @Test
    @DisplayName("Obtener paciente por usuario ID")
    void obtenerPorUsuarioId_existente_retornaPaciente() {
        paciente.setUsuarioId(50L);
        when(pacienteRepository.findByUsuarioId(50L)).thenReturn(Optional.of(paciente));

        Paciente resultado = service.obtenerPorUsuarioId(50L);

        assertThat(resultado.getUsuarioId()).isEqualTo(50L);
    }
}
