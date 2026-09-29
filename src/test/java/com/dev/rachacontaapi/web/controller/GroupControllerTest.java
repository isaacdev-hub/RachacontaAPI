package com.dev.rachacontaapi.web.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GroupControllerTest {

    @Autowired MockMvc mvc;

    // Cria um usuário novo (email único) e devolve o token JWT
    private String tokenDeNovoUsuario() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@teste.com";
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"123456\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String criarGrupo(String token, String nome) throws Exception {
        String body = mvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + nome + "\",\"description\":\"teste\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("Criar grupo com token válido retorna 201")
    void criarGrupoComToken() throws Exception {
        String token = tokenDeNovoUsuario();

        mvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Viagem\",\"description\":\"Praia\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Viagem"));
    }

    @Test
    @DisplayName("Criar grupo sem nome retorna 400")
    void criarGrupoSemNome() throws Exception {
        String token = tokenDeNovoUsuario();

        mvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").value("Nome do grupo é obrigatório"));
    }

    @Test
    @DisplayName("Lista apenas os grupos do usuário logado")
    void listarMeusGrupos() throws Exception {
        String token = tokenDeNovoUsuario();
        criarGrupo(token, "Viagem");
        criarGrupo(token, "Casa");

        mvc.perform(get("/api/groups").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Buscar grupo por id retorna 200")
    void buscarGrupoPorId() throws Exception {
        String token = tokenDeNovoUsuario();
        String id = criarGrupo(token, "Churrasco");

        mvc.perform(get("/api/groups/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Churrasco"));
    }

    @Test
    @DisplayName("Buscar grupo inexistente retorna 400")
    void buscarGrupoInexistente() throws Exception {
        String token = tokenDeNovoUsuario();

        mvc.perform(get("/api/groups/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Grupo não encontrado"));
    }
}