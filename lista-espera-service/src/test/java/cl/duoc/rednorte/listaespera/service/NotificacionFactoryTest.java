package cl.duoc.rednorte.listaespera.service;

import cl.duoc.rednorte.listaespera.model.Notificacion;
import cl.duoc.rednorte.listaespera.model.NotificacionEmail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("NotificacionFactory - Patron Factory Method (RF5)")
class NotificacionFactoryTest {

    private NotificacionFactory factory;

    @BeforeEach
    void setUp() {
        factory = new NotificacionFactory();
    }

    @Test
    @DisplayName("Crear notificacion EMAIL → retorna instancia correcta")
    void crear_email_retornaNotificacionEmail() {
        Notificacion notificacion = factory.crear("EMAIL");

        assertThat(notificacion).isInstanceOf(NotificacionEmail.class);
        assertThat(notificacion.getTipo()).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("Enviar notificacion EMAIL → no lanza excepcion")
    void notificacionEmail_enviar_ejecutaSinErrores() {
        Notificacion notificacion = factory.crear("EMAIL");

        assertThatNoException().isThrownBy(
                () -> notificacion.enviar("paciente@mail.com", "Su cita fue confirmada")
        );
    }

    @Test
    @DisplayName("Crear tipo desconocido → lanza IllegalArgumentException")
    void crear_tipoDesconocido_lanzaIllegalArgumentException() {
        assertThatThrownBy(() -> factory.crear("WHATSAPP"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WHATSAPP");
    }
}
