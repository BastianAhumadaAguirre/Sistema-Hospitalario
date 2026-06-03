package cl.duoc.rednorte.medico.service;

import cl.duoc.rednorte.medico.dto.MedicoDTO;
import cl.duoc.rednorte.medico.model.Medico;
import cl.duoc.rednorte.medico.repository.MedicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicoService - Pruebas unitarias")
class MedicoServiceTest {

    @Mock
    private MedicoRepository medicoRepository;

    @InjectMocks
    private MedicoService service;

    private Medico medico;
    private MedicoDTO dto;

    @BeforeEach
    void setUp() {
        medico = Medico.builder()
                .id(1L).rut("9876543-2").nombre("Carlos").apellido("Garcia")
                .especialidad("Cardiologia").email("dr.garcia@hospital.cl")
                .telefono("56922222222").numeroRegistro("REG-001").activo(true)
                .build();

        dto = MedicoDTO.builder()
                .rut("9876543-2").nombre("Carlos").apellido("Garcia")
                .especialidad("Cardiologia").email("dr.garcia@hospital.cl")
                .telefono("56922222222").numeroRegistro("REG-001")
                .build();
    }

    // ── Consultas de medicos ─────────────────────────────────────────────

    @Nested
    @DisplayName("Consultas de medicos")
    class Consultas {

        @Test
        @DisplayName("Listar todos los medicos del sistema")
        void listarTodos_retornaListaCompleta() {
            when(medicoRepository.findAll()).thenReturn(List.of(medico));

            List<Medico> resultado = service.listarTodos();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNombre()).isEqualTo("Carlos");
        }

        @Test
        @DisplayName("Obtener medico por ID existente")
        void obtenerPorId_existente_retornaMedico() {
            when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico));

            Medico resultado = service.obtenerPorId(1L);

            assertThat(resultado.getRut()).isEqualTo("9876543-2");
        }

        @Test
        @DisplayName("Obtener medico por ID inexistente → lanza excepcion")
        void obtenerPorId_noExistente_lanzaExcepcion() {
            when(medicoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerPorId(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("Listar medicos activos para asignacion de citas")
        void listarActivos_retornaSoloActivos() {
            when(medicoRepository.findByActivo(true)).thenReturn(List.of(medico));

            List<Medico> resultado = service.listarActivos();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getActivo()).isTrue();
        }
    }

    // ── Gestion de medicos ───────────────────────────────────────────────

    @Nested
    @DisplayName("Gestion de medicos")
    class GestionMedicos {

        @Test
        @DisplayName("Registrar medico nuevo → se guarda con activo=true")
        void crear_rutNuevo_guardaMedico() {
            when(medicoRepository.existsByRut("9876543-2")).thenReturn(false);
            when(medicoRepository.save(any())).thenReturn(medico);

            Medico resultado = service.crear(dto);

            assertThat(resultado.getNombre()).isEqualTo("Carlos");
            assertThat(resultado.getActivo()).isTrue();
            verify(medicoRepository).save(any());
        }

        @Test
        @DisplayName("Registrar medico con RUT duplicado → lanza excepcion")
        void crear_rutDuplicado_lanzaExcepcion() {
            when(medicoRepository.existsByRut("9876543-2")).thenReturn(true);

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("RUT");

            verify(medicoRepository, never()).save(any());
        }
    }
}
