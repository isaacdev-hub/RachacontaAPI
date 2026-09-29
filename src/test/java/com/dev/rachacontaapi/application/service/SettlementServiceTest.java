package com.dev.rachacontaapi.application.service;

import com.dev.rachacontaapi.application.dto.response.SettlementResponse;
import com.dev.rachacontaapi.domain.enums.SettlementStatus;
import com.dev.rachacontaapi.domain.model.Group;
import com.dev.rachacontaapi.domain.model.Settlement;
import com.dev.rachacontaapi.domain.model.User;
import com.dev.rachacontaapi.infrastructure.repository.*;
import com.dev.rachacontaapi.infrastructure.security.AuthenticatedUserResolver;
import com.dev.rachacontaapi.web.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock SettlementRepository settlementRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock ExpenseSplitRepository expenseSplitRepository;
    @Mock GroupRepository groupRepository;
    @Mock UserRepository userRepository;
    @Mock AuthenticatedUserResolver authenticatedUserResolver;

    @InjectMocks SettlementService service;

    private User user(String name) {
        User u = User.builder()
                .name(name)
                .email(name + "@teste.com")
                .passwordHash("x")
                .build();
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    @Test
    @DisplayName("A paga 90 dividido entre 3: B e C devem 30 cada para A")
    void deveCalcularLiquidacaoComMinimoDeTransferencias() {
        UUID groupId = UUID.randomUUID();
        User a = user("Ana");
        User b = user("Bruno");
        User c = user("Carla");

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(mock(Group.class)));
        when(expenseRepository.findTotalPaidPerUserInGroup(groupId))
                .thenReturn(List.<Object[]>of(new Object[]{a.getId(), new BigDecimal("90.00")}));
        when(expenseSplitRepository.findTotalOwedPerUserInGroup(groupId)).thenReturn(List.of(
                new Object[]{a.getId(), new BigDecimal("30.00")},
                new Object[]{b.getId(), new BigDecimal("30.00")},
                new Object[]{c.getId(), new BigDecimal("30.00")}));
        when(userRepository.getReferenceById(a.getId())).thenReturn(a);
        when(userRepository.getReferenceById(b.getId())).thenReturn(b);
        when(userRepository.getReferenceById(c.getId())).thenReturn(c);

        List<SettlementResponse> result = service.calculate(groupId);

        assertThat(result).hasSize(2);
        verify(settlementRepository).deleteByGroupIdAndStatus(groupId, SettlementStatus.PENDING);
        verify(settlementRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Grupo sem despesas não gera liquidação")
    void semDespesasNaoGeraLiquidacao() {
        UUID groupId = UUID.randomUUID();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(mock(Group.class)));
        when(expenseRepository.findTotalPaidPerUserInGroup(groupId)).thenReturn(List.of());
        when(expenseSplitRepository.findTotalOwedPerUserInGroup(groupId)).thenReturn(List.of());

        assertThat(service.calculate(groupId)).isEmpty();
    }

    @Test
    @DisplayName("Grupo inexistente lança exceção")
    void grupoInexistenteLancaExcecao() {
        UUID groupId = UUID.randomUUID();
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(groupId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Grupo não encontrado");
    }

    @Test
    @DisplayName("Apenas o recebedor pode confirmar a liquidação")
    void apenasRecebedorPodeConfirmar() {
        User recebedor = user("Ana");
        User outro = user("Bruno");
        Settlement s = Settlement.builder()
                .payer(outro).receiver(recebedor)
                .amount(new BigDecimal("30.00"))
                .status(SettlementStatus.PENDING)
                .build();
        UUID id = UUID.randomUUID();
        when(settlementRepository.findById(id)).thenReturn(Optional.of(s));
        when(authenticatedUserResolver.getCurrentUser()).thenReturn(outro);

        assertThatThrownBy(() -> service.confirm(id))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Apenas o recebedor pode confirmar esta liquidação");
        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Recebedor confirma liquidação com sucesso")
    void recebedorConfirmaComSucesso() {
        User recebedor = user("Ana");
        User pagador = user("Bruno");
        Settlement s = Settlement.builder()
                .payer(pagador).receiver(recebedor)
                .amount(new BigDecimal("30.00"))
                .status(SettlementStatus.PENDING)
                .build();
        UUID id = UUID.randomUUID();
        when(settlementRepository.findById(id)).thenReturn(Optional.of(s));
        when(authenticatedUserResolver.getCurrentUser()).thenReturn(recebedor);

        service.confirm(id);

        assertThat(s.getStatus()).isEqualTo(SettlementStatus.CONFIRMED);
        verify(settlementRepository).save(s);
    }
}