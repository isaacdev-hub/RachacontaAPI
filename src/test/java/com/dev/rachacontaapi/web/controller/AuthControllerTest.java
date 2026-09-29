package com.dev.rachacontaapi.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mvc;

    private String json(String email) {
        return """
            {"name":"Ana","email":"%s","password":"123456"}
            """.formatted(email);
    }

    @Test
    @DisplayName("Registro válido retorna 201")
    void registroValidoRetorna201() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("novo@teste.com")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Email inválido retorna 400")
    void emailInvalidoRetorna400() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email-invalido")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").value("Email inválido"));
    }

    @Test
    @DisplayName("Email duplicado retorna 400")
    void emailDuplicadoRetorna400() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("dup@teste.com")));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("dup@teste.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email já cadastrado"));
    }

    @Test
    @DisplayName("Endpoint protegido sem token é bloqueado")
    void endpointProtegidoSemTokenEBloqueado() throws Exception {
        mvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(r -> assertThat(r.getResponse().getStatus()).isIn(401, 403));
    }
}