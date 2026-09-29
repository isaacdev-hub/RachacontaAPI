package com.dev.rachacontaapi.application.service;

import com.dev.rachacontaapi.application.dto.request.CreateExpenseRequest;
import com.dev.rachacontaapi.application.dto.request.ExpenseSplitRequest;
import com.dev.rachacontaapi.domain.enums.SplitType;
import com.dev.rachacontaapi.domain.model.Expense;
import com.dev.rachacontaapi.domain.model.ExpenseSplit;
import com.dev.rachacontaapi.domain.model.Group;
import com.dev.rachacontaapi.domain.model.GroupMember;
import com.dev.rachacontaapi.domain.model.User;
import com.dev.rachacontaapi.infrastructure.repository.*;
import com.dev.rachacontaapi.infrastructure.security.AuthenticatedUserResolver;
import com.dev.rachacontaapi.web.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock ExpenseRepository expenseRepository;
    @Mock ExpenseSplitRepository expenseSplitRepository;
    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock UserRepository userRepository;
    @Mock AuthenticatedUserResolver authenticatedUserResolver;

    @InjectMocks ExpenseService service;

    @Captor ArgumentCaptor<List<ExpenseSplit>> splitsCaptor;

    private final UUID groupId = UUID.randomUUID();
    private final Group group = Group.builder().name("Viagem").build();

    private User user(String name) {
        User u = User.builder().name(name).email(name + "@teste.com").passwordHash("x").build();
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    private GroupMember membro(User u) {
        return GroupMember.builder().group(group).user(u).build();
    }

    private void mockPagadorEGrupo(User pagador) {
        when(authenticatedUserResolver.getCurrentUser()).thenReturn(pagador);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
    }

    @Test
    @DisplayName("EQUAL: R$ 90 entre 3 membros gera 3 divisões de R$ 30")
    void divisaoIgualitaria() {
        User ana = user("Ana"), bruno = user("Bruno"), carla = user("Carla");
        mockPagadorEGrupo(ana);
        when(groupMemberRepository.findByGroupId(groupId))
                .thenReturn(List.of(membro(ana), membro(bruno), membro(carla)));

        service.create(groupId, new CreateExpenseRequest("Jantar", new BigDecimal("90.00"), SplitType.EQUAL, null));

        verify(expenseRepository).save(any(Expense.class));
        verify(expenseSplitRepository).saveAll(splitsCaptor.capture());
        assertThat(splitsCaptor.getValue())
                .hasSize(3)
                .allSatisfy(s -> assertThat(s.getAmountOwed()).isEqualByComparingTo("30.00"));
    }

    @Test
    @DisplayName("EQUAL: R$ 100 entre 3 arredonda para R$ 33,33")
    void divisaoIgualitariaComArredondamento() {
        User ana = user("Ana"), bruno = user("Bruno"), carla = user("Carla");
        mockPagadorEGrupo(ana);
        when(groupMemberRepository.findByGroupId(groupId))
                .thenReturn(List.of(membro(ana), membro(bruno), membro(carla)));

        service.create(groupId, new CreateExpenseRequest("Mercado", new BigDecimal("100.00"), SplitType.EQUAL, null));

        verify(expenseSplitRepository).saveAll(splitsCaptor.capture());
        assertThat(splitsCaptor.getValue())
                .allSatisfy(s -> assertThat(s.getAmountOwed()).isEqualByComparingTo("33.33"));
    }

    @Test
    @DisplayName("CUSTOM sem divisões lança exceção")
    void customSemDivisoes() {
        mockPagadorEGrupo(user("Ana"));

        assertThatThrownBy(() -> service.create(groupId,
                new CreateExpenseRequest("Uber", new BigDecimal("50.00"), SplitType.CUSTOM, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Splits customizados são obrigatórios para CUSTOM");
    }

    @Test
    @DisplayName("CUSTOM com soma diferente do total lança exceção")
    void customComSomaErrada() {
        mockPagadorEGrupo(user("Ana"));
        List<ExpenseSplitRequest> splits = List.of(
                new ExpenseSplitRequest(UUID.randomUUID(), new BigDecimal("50.00")),
                new ExpenseSplitRequest(UUID.randomUUID(), new BigDecimal("30.00")));

        assertThatThrownBy(() -> service.create(groupId,
                new CreateExpenseRequest("Hotel", new BigDecimal("90.00"), SplitType.CUSTOM, splits)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A soma das divisões não bate com o valor total da despesa");
        verify(expenseSplitRepository, never()).save(any());
    }

    @Test
    @DisplayName("CUSTOM válido salva uma divisão por usuário")
    void customValido() {
        User ana = user("Ana"), bruno = user("Bruno");
        mockPagadorEGrupo(ana);
        when(userRepository.findById(ana.getId())).thenReturn(Optional.of(ana));
        when(userRepository.findById(bruno.getId())).thenReturn(Optional.of(bruno));
        List<ExpenseSplitRequest> splits = List.of(
                new ExpenseSplitRequest(ana.getId(), new BigDecimal("60.00")),
                new ExpenseSplitRequest(bruno.getId(), new BigDecimal("30.00")));

        service.create(groupId, new CreateExpenseRequest("Hotel", new BigDecimal("90.00"), SplitType.CUSTOM, splits));

        verify(expenseSplitRepository, times(2)).save(any(ExpenseSplit.class));
    }

    @Test
    @DisplayName("Despesa em grupo inexistente lança exceção")
    void grupoInexistente() {
        when(authenticatedUserResolver.getCurrentUser()).thenReturn(user("Ana"));
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(groupId,
                new CreateExpenseRequest("Jantar", new BigDecimal("90.00"), SplitType.EQUAL, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Grupo não encontrado");
        verify(expenseRepository, never()).save(any());
    }
}