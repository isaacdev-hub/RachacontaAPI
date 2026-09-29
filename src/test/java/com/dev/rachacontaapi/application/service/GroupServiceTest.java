package com.dev.rachacontaapi.application.service;

import com.dev.rachacontaapi.application.dto.request.CreateGroupRequest;
import com.dev.rachacontaapi.application.dto.response.GroupResponse;
import com.dev.rachacontaapi.domain.enums.GroupRole;
import com.dev.rachacontaapi.domain.model.Group;
import com.dev.rachacontaapi.domain.model.GroupMember;
import com.dev.rachacontaapi.domain.model.User;
import com.dev.rachacontaapi.infrastructure.repository.GroupMemberRepository;
import com.dev.rachacontaapi.infrastructure.repository.GroupRepository;
import com.dev.rachacontaapi.infrastructure.repository.UserRepository;
import com.dev.rachacontaapi.infrastructure.security.AuthenticatedUserResolver;
import com.dev.rachacontaapi.web.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock UserRepository userRepository;
    @Mock AuthenticatedUserResolver authenticatedUserResolver;

    @InjectMocks GroupService service;

    private final UUID groupId = UUID.randomUUID();

    private User user(String name) {
        User u = User.builder().name(name).email(name + "@teste.com").passwordHash("x").build();
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    private GroupMember membro(GroupRole role) {
        return GroupMember.builder().role(role).build();
    }

    @Test
    @DisplayName("Criador do grupo entra automaticamente como ADMIN")
    void criadorEntraComoAdmin() {
        User ana = user("Ana");
        when(authenticatedUserResolver.getCurrentUser()).thenReturn(ana);

        GroupResponse response = service.create(new CreateGroupRequest("Viagem", "Praia"));

        assertThat(response.name()).isEqualTo("Viagem");
        verify(groupRepository).save(any(Group.class));
        ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(GroupRole.ADMIN);
        assertThat(captor.getValue().getUser()).isEqualTo(ana);
    }

    @Test
    @DisplayName("Lista apenas os grupos do usuário logado")
    void listaGruposDoUsuario() {
        User ana = user("Ana");
        when(authenticatedUserResolver.getCurrentUser()).thenReturn(ana);
        when(groupMemberRepository.findByUserId(ana.getId())).thenReturn(List.of(
                GroupMember.builder().group(Group.builder().name("Viagem").build()).build(),
                GroupMember.builder().group(Group.builder().name("Casa").build()).build()));

        List<GroupResponse> grupos = service.listMyGroups();

        assertThat(grupos).hasSize(2);
    }

    @Test
    @DisplayName("Buscar grupo inexistente lança exceção")
    void buscarGrupoInexistente() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(groupId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Grupo não encontrado");
    }

    @Test
    @DisplayName("Usuário fora do grupo não pode adicionar membros")
    void usuarioForaDoGrupoNaoAdiciona() {
        User ana = user("Ana");
        when(authenticatedUserResolver.getCurrentUser()).thenReturn(ana);
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, ana.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addMember(groupId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Usuário não pertence ao grupo");
    }

    @Test
    @DisplayName("Membro comum não pode adicionar membros")
    void membroComumNaoAdiciona() {
        User ana = user("Ana");
        when(authenticatedUserResolver.getCurrentUser()).thenReturn(ana);
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, ana.getId()))
                .thenReturn(Optional.of(membro(GroupRole.MEMBER)));

        assertThatThrownBy(() -> service.addMember(groupId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Apenas administradores podem adicionar membros");
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não adiciona quem já é membro")
    void naoAdicionaMembroDuplicado() {
        User ana = user("Ana");
        UUID brunoId = UUID.randomUUID();
        when(authenticatedUserResolver.getCurrentUser()).thenReturn(ana);
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, ana.getId()))
                .thenReturn(Optional.of(membro(GroupRole.ADMIN)));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, brunoId)).thenReturn(true);

        assertThatThrownBy(() -> service.addMember(groupId, brunoId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Usuário já é membro do grupo");
    }

    @Test
    @DisplayName("Admin adiciona novo membro com papel MEMBER")
    void adminAdicionaMembro() {
        User ana = user("Ana");
        User bruno = user("Bruno");
        Group group = Group.builder().name("Viagem").build();
        when(authenticatedUserResolver.getCurrentUser()).thenReturn(ana);
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, ana.getId()))
                .thenReturn(Optional.of(membro(GroupRole.ADMIN)));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, bruno.getId())).thenReturn(false);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findById(bruno.getId())).thenReturn(Optional.of(bruno));

        service.addMember(groupId, bruno.getId());

        ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(GroupRole.MEMBER);
        assertThat(captor.getValue().getUser()).isEqualTo(bruno);
    }
}