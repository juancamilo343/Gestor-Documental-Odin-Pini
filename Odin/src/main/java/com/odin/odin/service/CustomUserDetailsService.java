package com.odin.odin.service;

import com.odin.odin.model.Permisos;
import com.odin.odin.model.Roles;
import com.odin.odin.model.Usuarios;
import com.odin.odin.repository.RolesRepository;
import com.odin.odin.repository.RolPermisosRepository;
import com.odin.odin.repository.UsuariosRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuariosRepository usuariosRepository;
    private final RolesRepository rolesRepository;
    private final RolPermisosRepository rolPermisosRepository;

    public CustomUserDetailsService(UsuariosRepository usuariosRepository,
                                    RolesRepository rolesRepository,
                                    RolPermisosRepository rolPermisosRepository) {
        this.usuariosRepository = usuariosRepository;
        this.rolesRepository = rolesRepository;
        this.rolPermisosRepository = rolPermisosRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        if (login == null || login.trim().isEmpty()) {
            throw new UsernameNotFoundException("Debe ingresar correo, identificación o nombre");
        }

        String q = login.trim();
        Usuarios usuario = usuariosRepository.findByLoginFlexible(q).orElse(null);
        if (usuario == null && q.contains("@")) {
            usuario = usuariosRepository.findByCorreoIgnoreCase(q).orElse(null);
        }
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }

        String estado = usuario.getEstado() == null ? "" : usuario.getEstado().trim();
        if (!estado.equalsIgnoreCase("activo")) {
            throw new UsernameNotFoundException("Usuario inactivo");
        }
        if (usuario.getId_rol() == null) {
            throw new UsernameNotFoundException("El usuario no tiene rol asignado");
        }

        Roles rol = rolesRepository.findById(usuario.getId_rol())
                .orElseThrow(() -> new UsernameNotFoundException("Rol no encontrado"));
        String estadoRol = rol.getEstado() == null ? "" : rol.getEstado().trim();
        if (!estadoRol.equalsIgnoreCase("activo")) {
            throw new UsernameNotFoundException("El rol se encuentra inactivo");
        }

        List<GrantedAuthority> autoridades = new ArrayList<>();
        String rolNorm = normalizarRol(rol.getRol());
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rolNorm));
        // Permiso amplio para administrador del sistema
        if ("ADMINISTRADOR".equals(rolNorm) || "ADMIN".equals(rolNorm)) {
            autoridades.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            autoridades.add(new SimpleGrantedAuthority("admin_usuarios"));
            autoridades.add(new SimpleGrantedAuthority("admin_roles"));
            autoridades.add(new SimpleGrantedAuthority("ver_reportes"));
            autoridades.add(new SimpleGrantedAuthority("ver_bitacora"));
            autoridades.add(new SimpleGrantedAuthority("ver_radicados"));
            autoridades.add(new SimpleGrantedAuthority("ver_tramites"));
            autoridades.add(new SimpleGrantedAuthority("ver_dependencias"));
            autoridades.add(new SimpleGrantedAuthority("admin_ccd"));
            autoridades.add(new SimpleGrantedAuthority("trasladar_radicado"));
        }

        try {
            List<Permisos> permisos = rolPermisosRepository.buscarPermisosPorRol(usuario.getId_rol());
            if (permisos != null) {
                for (Permisos permiso : permisos) {
                    if (permiso.getNombre() == null) continue;
                    String nombrePermiso = permiso.getNombre().trim().toLowerCase(Locale.ROOT);
                    if (!nombrePermiso.isEmpty()) {
                        autoridades.add(new SimpleGrantedAuthority(nombrePermiso));
                    }
                }
            }
        } catch (Exception ignored) {
            // Tabla de permisos vacía o no disponible: se mantienen ROLE_*
        }

        String correo = usuario.getCorreo() != null ? usuario.getCorreo() : q;
        return new OdinUserDetails(
                correo,
                usuario.getPassword(),
                usuario.getNombre(),
                correo,
                usuario.getId_usuario(),
                autoridades,
                true
        );
    }

    private String normalizarRol(String rol) {
        if (rol == null || rol.trim().isEmpty()) return "CONSULTA";
        return rol.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
    }
}
