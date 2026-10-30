package com.odin.odin.controller;

import com.odin.odin.service.PasswordRecoveryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// @Controller
public class PasswordRecoveryController {
    private final PasswordRecoveryService recoveryService;
    public PasswordRecoveryController(PasswordRecoveryService recoveryService) { this.recoveryService = recoveryService; }
    @GetMapping("/recuperar-password")
    public String recuperarPassword() { return "auth/recuperar-password"; }
    @PostMapping("/recuperar-password")
    public String enviarCodigo(@RequestParam String correo, HttpSession session, Model model) {
        String normalizado = correo == null ? "" : correo.trim();
        try { recoveryService.solicitarRecuperacion(normalizado); }
        catch (Exception e) { model.addAttribute("error", "No fue posible enviar el código. Verifica la configuración de correo e intenta nuevamente."); return "auth/recuperar-password"; }
        session.setAttribute("recoveryEmail", normalizado);
        model.addAttribute("mensaje", "Si el correo está registrado, recibirás un código de recuperación.");
        return "auth/verificar-codigo";
    }
    @GetMapping("/recuperar-password/verificar")
    public String verificar(HttpSession session) { return session.getAttribute("recoveryEmail") == null ? "redirect:/recuperar-password" : "auth/verificar-codigo"; }
    @PostMapping("/recuperar-password/verificar")
    public String verificarCodigo(@RequestParam String codigo, HttpSession session, Model model) {
        String correo = (String) session.getAttribute("recoveryEmail");
        if (correo == null) return "redirect:/recuperar-password";
        if (!recoveryService.validarCodigo(correo, codigo)) { model.addAttribute("error", "El código es incorrecto o ha expirado."); return "auth/verificar-codigo"; }
        session.setAttribute("recoveryCode", codigo.trim()); session.setAttribute("recoveryVerified", true);
        return "redirect:/recuperar-password/nueva";
    }
    @GetMapping("/recuperar-password/nueva")
    public String nuevaPassword(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("recoveryVerified")) && session.getAttribute("recoveryEmail") != null && session.getAttribute("recoveryCode") != null ? "auth/nueva-password" : "redirect:/recuperar-password";
    }
    @PostMapping("/recuperar-password/nueva")
    public String guardarPassword(@RequestParam String password, @RequestParam String confirmarPassword, HttpSession session, Model model) {
        String correo=(String)session.getAttribute("recoveryEmail"), codigo=(String)session.getAttribute("recoveryCode");
        if (!Boolean.TRUE.equals(session.getAttribute("recoveryVerified")) || correo == null || codigo == null) return "redirect:/recuperar-password";
        if (password == null || password.length() < 8) { model.addAttribute("error", "La contraseña debe tener mínimo 8 caracteres."); return "auth/nueva-password"; }
        if (!password.equals(confirmarPassword)) { model.addAttribute("error", "Las contraseñas no coinciden."); return "auth/nueva-password"; }
        if (!recoveryService.cambiarPassword(correo, codigo, password)) { limpiar(session); return "redirect:/recuperar-password?expirado=true"; }
        limpiar(session); return "redirect:/login?reset=true";
    }
    private void limpiar(HttpSession session) { session.removeAttribute("recoveryEmail"); session.removeAttribute("recoveryCode"); session.removeAttribute("recoveryVerified"); }
}
