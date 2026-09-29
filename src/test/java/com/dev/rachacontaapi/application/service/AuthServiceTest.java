package com.dev.rachacontaapi.application.service;

import com.dev.rachacontaapi.application.dto.request.RegisterRequest;
import com.dev.rachacontaapi.domain.model.User;
import com.dev.rachacontaapi.infrastructure.repository.UserRepository;
import com.dev.rachacontaapi.infrastructure.security.JwtUtil;
import com.dev.rachacontaapi.web.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    @Test
    @DisplayName("Não permite cadastrar email duplicado")
    void naoPermiteEmailDuplicado() {
        when(userRepository.existsByEmail("ana@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ana", "ana@teste.com", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email já cadastrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registra usuário com senha criptografada")
    void registraComSenhaCriptografada() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("HASH");
        when(jwtUtil.generateToken("ana@teste.com")).thenReturn("token-fake");

        authService.register(new RegisterRequest("Ana", "ana@teste.com", "123456"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("HASH");
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@teste.com");
    }
}