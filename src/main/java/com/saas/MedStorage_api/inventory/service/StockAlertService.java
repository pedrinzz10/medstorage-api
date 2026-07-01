package com.saas.MedStorage_api.inventory.service;

import com.saas.MedStorage_api.batch.entity.ProductBatch;
import com.saas.MedStorage_api.batch.repository.ProductBatchRepository;
import com.saas.MedStorage_api.inventory.dto.InventoryStatusResponse;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import com.saas.MedStorage_api.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Falha de envio nao deve interromper o fluxo do sistema (mesma regra aplicada
 * em OrderNotificationService) - captura e loga em vez de propagar.
 */
@Slf4j
@Service
public class StockAlertService {

    private static final List<UserRole> STAFF_ROLES = List.of(UserRole.ADMIN, UserRole.GERENTE_ESTOQUE);

    private final InventoryService inventoryService;
    private final ProductBatchRepository productBatchRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final int expiryAlertDays;

    public StockAlertService(
            InventoryService inventoryService,
            ProductBatchRepository productBatchRepository,
            UserRepository userRepository,
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.mail.from-name}") String fromName,
            @Value("${app.stock.expiry-alert-days:30}") int expiryAlertDays) {
        this.inventoryService = inventoryService;
        this.productBatchRepository = productBatchRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.expiryAlertDays = expiryAlertDays;
    }

    /**
     * Verifica produtos com status CRITICO e lotes próximos do vencimento, e
     * notifica a equipe de estoque/admin em um único email. Não faz nada se
     * nenhuma das duas condições for encontrada.
     *
     * @return quantidade de produtos em estado CRITICO encontrados (0 pode
     *         ainda assim ter disparado o email, se houver lotes vencendo)
     */
    public int checkAndNotify() {
        List<InventoryStatusResponse> criticos = inventoryService.getStatus().stream()
                .filter(i -> "CRITICO".equals(i.statusEstoque()))
                .toList();

        List<ProductBatch> vencendo = productBatchRepository.findAll().stream()
                .filter(b -> b.getQuantidade() > 0)
                .filter(b -> !b.getValidade().isAfter(LocalDate.now().plusDays(expiryAlertDays)))
                .sorted(Comparator.comparing(ProductBatch::getValidade))
                .toList();

        if (criticos.isEmpty() && vencendo.isEmpty()) {
            log.debug("Nenhum produto crítico ou lote vencendo, alerta de estoque nao enviado");
            return 0;
        }

        List<String> recipients = userRepository.findByRoleInAndAtivoTrue(STAFF_ROLES).stream()
                .map(User::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .toList();

        if (recipients.isEmpty()) {
            log.warn("Nenhum admin/gerente ativo encontrado para alerta de estoque ({} críticos, {} lotes vencendo)",
                    criticos.size(), vencendo.size());
            return criticos.size();
        }

        String subject = buildSubject(criticos, vencendo);
        String body = buildBody(criticos, vencendo);
        recipients.forEach(email -> send(email, subject, body));
        return criticos.size();
    }

    private String buildSubject(List<InventoryStatusResponse> criticos, List<ProductBatch> vencendo) {
        if (!criticos.isEmpty() && !vencendo.isEmpty()) {
            return "Alerta de estoque: " + criticos.size() + " produto(s) crítico(s), " + vencendo.size() + " lote(s) vencendo";
        }
        if (!criticos.isEmpty()) {
            return "Alerta de estoque baixo: " + criticos.size() + " produto(s) crítico(s)";
        }
        return "Alerta de lotes próximos do vencimento: " + vencendo.size() + " lote(s)";
    }

    private String buildBody(List<InventoryStatusResponse> criticos, List<ProductBatch> vencendo) {
        StringBuilder body = new StringBuilder();
        if (!criticos.isEmpty()) {
            body.append("Os produtos abaixo estão com estoque disponível igual ou abaixo do mínimo definido:\n\n");
            for (InventoryStatusResponse item : criticos) {
                body.append(" - ").append(item.nome())
                        .append(" (").append(item.sku()).append(")")
                        .append(": disponível ").append(item.disponivel())
                        .append(", mínimo ").append(item.estoqueMinimo())
                        .append('\n');
            }
            body.append("\nRecomenda-se repor o estoque desses produtos o quanto antes.\n");
        }
        if (!vencendo.isEmpty()) {
            if (!criticos.isEmpty()) {
                body.append('\n');
            }
            body.append("Os lotes abaixo estão próximos do vencimento (até ").append(expiryAlertDays).append(" dias):\n\n");
            for (ProductBatch batch : vencendo) {
                body.append(" - ").append(batch.getProduct().getNome())
                        .append(" — lote ").append(batch.getLote())
                        .append(": vence em ").append(batch.getValidade())
                        .append(", quantidade ").append(batch.getQuantidade())
                        .append('\n');
            }
            body.append("\nAvalie priorizar a saída desses lotes (FEFO) ou providenciar o descarte adequado.");
        }
        return body.toString();
    }

    private void send(String recipient, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromAddress + ">");
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Falha ao enviar alerta de estoque para {}: {}", recipient, ex.getMessage());
        }
    }
}
