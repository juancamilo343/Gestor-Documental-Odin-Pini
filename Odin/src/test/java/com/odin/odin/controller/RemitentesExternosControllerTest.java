package com.odin.odin.controller;

import com.odin.odin.model.RemitentesExternos;
import com.odin.odin.model.Usuarios;
import com.odin.odin.repository.RemitentesExternosRepository;
import com.odin.odin.repository.UsuariosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemitentesExternosControllerTest {

    @Mock
    private RemitentesExternosRepository remitentesRepository;

    @Mock
    private UsuariosRepository usuariosRepository;

    @InjectMocks
    private RemitentesExternosController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "usuariosRepository", usuariosRepository);
        ReflectionTestUtils.setField(controller, "remitentesRepository", remitentesRepository);
    }

    @Test
    @DisplayName("Debe autocompletar desde Gestión de Usuarios si la cédula existe")
    void buscarPorDocumento_EncuentraEnGestionUsuarios() {
        String cedula = "1098765432";
        Usuarios usuarioMock = Usuarios.builder()
                .id_usuario(15L)
                .nombre("Carlos Alberto Pérez")
                .tipo_identificacion("CC")
                .num_identificacion(cedula)
                .correo("carlos.perez@sena.edu.co")
                .telefono("3112345678")
                .direccion("Calle 100 # 15-20")
                .estado("ACTIVO")
                .build();

        when(usuariosRepository.findByLoginFlexible(cedula)).thenReturn(Optional.of(usuarioMock));

        ResponseEntity<?> response = controller.buscarPorDocumento(cedula, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("USUARIO", body.get("origen"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertEquals("Carlos Alberto Pérez", data.get("nombre"));
        assertEquals("carlos.perez@sena.edu.co", data.get("correo"));
        assertEquals("3112345678", data.get("telefono"));
        assertEquals("Calle 100 # 15-20", data.get("direccion"));

        // No debe consultar tabla de remitentes si ya lo encontró en usuarios
        verify(remitentesRepository, never()).findByNumIdentificacion(anyString());
    }

    @Test
    @DisplayName("Debe consultar en Remitentes Externos si no está en Gestión de Usuarios")
    void buscarPorDocumento_EncuentraEnRemitentesExternos() {
        String cedula = "80123456";
        when(usuariosRepository.findByLoginFlexible(cedula)).thenReturn(Optional.empty());

        RemitentesExternos remMock = RemitentesExternos.builder()
                .id_remitente(8L)
                .nombre("Empresa Constructora")
                .apellido("S.A.S")
                .num_identificacion(cedula)
                .correo("contacto@constructora.com")
                .telefono("6014445566")
                .direccion("Carrera 7 # 72-10")
                .build();

        when(remitentesRepository.findByNumIdentificacion(cedula)).thenReturn(Optional.of(remMock));

        ResponseEntity<?> response = controller.buscarPorDocumento(cedula, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("REMITENTE_EXTERNO", body.get("origen"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertEquals("Empresa Constructora S.A.S", data.get("nombre_completo"));
        assertEquals("contacto@constructora.com", data.get("correo"));
    }

    @Test
    @DisplayName("Debe retornar 404 informativo cuando la persona no está registrada")
    void buscarPorDocumento_NoEncontrado() {
        String cedula = "9999999999";
        when(usuariosRepository.findByLoginFlexible(cedula)).thenReturn(Optional.empty());
        when(remitentesRepository.findByNumIdentificacion(cedula)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.buscarPorDocumento(cedula, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("NO_ENCONTRADO", body.get("origen"));
        assertTrue(body.get("message").toString().contains("manualmente"));
    }

    @Test
    @DisplayName("Debe retornar 400 cuando el documento está vacío")
    void buscarPorDocumento_Vacio() {
        ResponseEntity<?> response = controller.buscarPorDocumento("   ", null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
