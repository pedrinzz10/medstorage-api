package com.saas.MedStorage_api.inventory;

import com.saas.MedStorage_api.batch.entity.ProductBatch;
import com.saas.MedStorage_api.batch.repository.ProductBatchRepository;
import com.saas.MedStorage_api.inventory.dto.InventoryStatusResponse;
import com.saas.MedStorage_api.inventory.service.InventoryService;
import com.saas.MedStorage_api.inventory.service.StockAlertService;
import com.saas.MedStorage_api.product.entity.Product;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import com.saas.MedStorage_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAlertServiceTest {

    @Mock
    private InventoryService inventoryService;
    @Mock
    private ProductBatchRepository productBatchRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JavaMailSender mailSender;

    private InventoryStatusResponse critico(String nome) {
        return new InventoryStatusResponse(UUID.randomUUID(), nome, "SKU-" + nome, 2, 2, 0, 10,
                "CRITICO", BigDecimal.TEN, "unidade");
    }

    private InventoryStatusResponse ok(String nome) {
        return new InventoryStatusResponse(UUID.randomUUID(), nome, "SKU-" + nome, 100, 100, 0, 10,
                "OK", BigDecimal.TEN, "unidade");
    }

    private ProductBatch batch(String lote, int diasValidade, int quantidade) {
        Product produto = Product.builder().id(UUID.randomUUID()).nome("Luva").build();
        return ProductBatch.builder().id(UUID.randomUUID()).product(produto)
                .lote(lote).validade(LocalDate.now().plusDays(diasValidade)).quantidade(quantidade).build();
    }

    private StockAlertService service() {
        return new StockAlertService(inventoryService, productBatchRepository, userRepository, mailSender,
                "no-reply@distribuidor.com", "MedStorage", 30);
    }

    private void staffAvailable() {
        User admin = User.builder().id(UUID.randomUUID()).email("admin@distribuidor.com").role(UserRole.ADMIN).ativo(true).build();
        when(userRepository.findByRoleInAndAtivoTrue(any())).thenReturn(List.of(admin));
    }

    @Test
    void checkAndNotify_withNoCriticalProductsAndNoExpiringBatches_sendsNoEmailAndReturnsZero() {
        when(inventoryService.getStatus()).thenReturn(List.of(ok("Luva")));

        int result = service().checkAndNotify();

        assertEquals(0, result);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void checkAndNotify_withCriticalProducts_sendsEmailToEachStaffMember() {
        when(inventoryService.getStatus()).thenReturn(List.of(critico("Luva"), ok("Máscara")));
        User admin = User.builder().id(UUID.randomUUID()).email("admin@distribuidor.com").role(UserRole.ADMIN).ativo(true).build();
        User gerente = User.builder().id(UUID.randomUUID()).email("gerente@distribuidor.com").role(UserRole.GERENTE_ESTOQUE).ativo(true).build();
        when(userRepository.findByRoleInAndAtivoTrue(any())).thenReturn(List.of(admin, gerente));

        int result = service().checkAndNotify();

        assertEquals(1, result);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());
        List<String> recipients = captor.getAllValues().stream()
                .map(m -> m.getTo()[0])
                .toList();
        assertEquals(List.of("admin@distribuidor.com", "gerente@distribuidor.com"), recipients);
    }

    @Test
    void checkAndNotify_withCriticalProductsButNoStaff_sendsNoEmailButReturnsCount() {
        when(inventoryService.getStatus()).thenReturn(List.of(critico("Luva")));
        when(userRepository.findByRoleInAndAtivoTrue(any())).thenReturn(List.of());

        int result = service().checkAndNotify();

        assertEquals(1, result);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void checkAndNotify_withExpiringBatchButNoCriticalStock_sendsEmailAndReturnsZero() {
        when(inventoryService.getStatus()).thenReturn(List.of(ok("Luva")));
        when(productBatchRepository.findAll()).thenReturn(List.of(batch("L1", 5, 20)));
        staffAvailable();

        int result = service().checkAndNotify();

        assertEquals(0, result);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void checkAndNotify_ignoresBatchesFarFromExpiryOrWithZeroQuantity() {
        when(inventoryService.getStatus()).thenReturn(List.of(ok("Luva")));
        when(productBatchRepository.findAll()).thenReturn(List.of(
                batch("L-LONGE", 90, 20),
                batch("L-VAZIO", 5, 0)));

        int result = service().checkAndNotify();

        assertEquals(0, result);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void checkAndNotify_withBothCriticalAndExpiring_sendsSingleCombinedEmail() {
        when(inventoryService.getStatus()).thenReturn(List.of(critico("Luva")));
        when(productBatchRepository.findAll()).thenReturn(List.of(batch("L1", 5, 20)));
        staffAvailable();

        int result = service().checkAndNotify();

        assertEquals(1, result);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
