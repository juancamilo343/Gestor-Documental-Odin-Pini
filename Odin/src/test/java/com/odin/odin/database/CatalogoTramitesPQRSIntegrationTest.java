package com.odin.odin.database;

import com.odin.odin.OdinApplication;
import com.odin.odin.util.PlazoUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CatalogoTramitesPQRSIntegrationTest {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPass;

    @BeforeAll
    static void setup() throws Exception {
        Method m = OdinApplication.class.getDeclaredMethod("loadDotEnv");
        m.setAccessible(true);
        m.invoke(null);

        dbUrl = System.getProperty("SUPABASE_DB_URL", System.getenv("SUPABASE_DB_URL"));
        dbUser = System.getProperty("SUPABASE_DB_USERNAME", System.getenv("SUPABASE_DB_USERNAME"));
        dbPass = System.getProperty("SUPABASE_DB_PASSWORD", System.getenv("SUPABASE_DB_PASSWORD"));

        if (dbUrl == null || dbUser == null || dbPass == null) {
            try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("SUPABASE_DB_URL=")) dbUrl = line.substring(16).trim();
                    else if (line.startsWith("SUPABASE_DB_USERNAME=")) dbUser = line.substring(21).trim();
                    else if (line.startsWith("SUPABASE_DB_PASSWORD=")) dbPass = line.substring(21).trim();
                }
            }
        }

        assertNotNull(dbUrl, "SUPABASE_DB_URL debe estar disponible");
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPass);
    }

    @Test
    @DisplayName("Verificar que los cuatro trámites existen en la BD con 15 días y activo=true, y PQRS está inactivo")
    void verificarCatalogoTramitesEnBD() throws Exception {
        try (Connection conn = getConnection()) {
            // Verificar PQRS id=1 (inactivo)
            try (PreparedStatement ps = conn.prepareStatement("SELECT nombre, dias_respuesta, activo FROM tramites WHERE id_tramite = 1")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "PQRS ID 1 debe existir");
                    assertEquals("PQRS", rs.getString("nombre"));
                    assertFalse(rs.getBoolean("activo"), "PQRS debe estar inactivo para no aparecer en nuevas radicaciones");
                }
            }

            // Verificar los cuatro trámites individuales
            long[] ids = {5L, 24L, 25L, 26L};
            String[] nombresEsperados = {"Petición", "Queja", "Reclamo", "Sugerencia"};

            for (int i = 0; i < ids.length; i++) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT nombre, dias_respuesta, activo FROM tramites WHERE id_tramite = ?")) {
                    ps.setLong(1, ids[i]);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next(), "Trámite ID " + ids[i] + " debe existir");
                        assertEquals(nombresEsperados[i], rs.getString("nombre"));
                        assertEquals(15, rs.getInt("dias_respuesta"));
                        assertTrue(rs.getBoolean("activo"), "Trámite " + nombresEsperados[i] + " debe estar activo");
                    }
                }
            }

            // Verificar que no se muestran trámites inactivos al consultar activos
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_tramite, nombre FROM tramites WHERE activo = true ORDER BY nombre ASC")) {
                List<String> activos = new ArrayList<>();
                while (rs.next()) {
                    activos.add(rs.getString("nombre"));
                }
                assertFalse(activos.contains("PQRS"), "El catálogo activo no debe incluir PQRS agrupado");
                assertTrue(activos.contains("Petición"), "El catálogo activo debe incluir Petición");
                assertTrue(activos.contains("Queja"), "El catálogo activo debe incluir Queja");
                assertTrue(activos.contains("Reclamo"), "El catálogo activo debe incluir Reclamo");
                assertTrue(activos.contains("Sugerencia"), "El catálogo activo debe incluir Sugerencia");
            }
        }
    }

    @Test
    @DisplayName("Verificar integridad de los 30 radicados históricos asociados a id_tramite = 1")
    void verificarIntegridadRadicadosHistoricos() throws Exception {
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM radicados WHERE id_tramite = 1")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    int total = rs.getInt(1);
                    assertEquals(30, total, "Deben existir exactamente 30 radicados históricos para id_tramite = 1");
                }
            }
        }
    }

    @Test
    @DisplayName("Simular radicación real para cada uno de los 4 trámites con persistencia del ID real y cálculo hábil")
    void simularRadicacionConCuatroTramites() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate vencimientoEsperado = PlazoUtil.calcularFechaLimiteHabiles(hoy, 15);

        long[] idsTramite = {5L, 24L, 25L, 26L};
        String[] tramites = {"Petición", "Queja", "Reclamo", "Sugerencia"};

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < idsTramite.length; i++) {
                    long idTr = idsTramite[i];
                    String nombreTr = tramites[i];
                    String radNumero = "TEST-PQRS-2026-" + idTr;

                    // Insertar radicado de prueba
                    String sql = "INSERT INTO radicados (numero_radicado, fecha_radicado, fecha_vencimiento, fecha_limite, " +
                            "id_tramite, asunto, id_dependencia, id_estado, id_usuario, medio_recepcion, prioridad, remitente) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 2, 1, 1, 'VENTANILLA_VIRTUAL', 'MEDIA', ?) RETURNING id_radicado";

                    long idRadicadoCreado;
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, radNumero);
                        ps.setString(2, hoy.toString());
                        ps.setString(3, vencimientoEsperado.toString());
                        ps.setString(4, vencimientoEsperado.toString());
                        ps.setLong(5, idTr);
                        ps.setString(6, "Radicado de prueba para " + nombreTr);
                        ps.setString(7, "Ciudadano Solicitante");

                        try (ResultSet rs = ps.executeQuery()) {
                            assertTrue(rs.next());
                            idRadicadoCreado = rs.getLong(1);
                            assertTrue(idRadicadoCreado > 0);
                        }
                    }

                    // Verificar en BD que id_tramite guardado es el ID real
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT id_tramite, fecha_vencimiento, fecha_limite FROM radicados WHERE id_radicado = ?")) {
                        ps.setLong(1, idRadicadoCreado);
                        try (ResultSet rs = ps.executeQuery()) {
                            assertTrue(rs.next());
                            assertEquals(idTr, rs.getLong("id_tramite"), "Debe guardarse el ID real del trámite " + nombreTr);
                            assertEquals(vencimientoEsperado.toString(), rs.getString("fecha_vencimiento").substring(0, 10));
                            assertEquals(vencimientoEsperado.toString(), rs.getString("fecha_limite").substring(0, 10));
                        }
                    }
                }
                // Rollback para no dejar datos ficticios en la base de datos de producción/desarrollo
                conn.rollback();
                System.out.println("Pruebas de radicación para los 4 trámites verificadas y revertidas limpiamente.");
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
