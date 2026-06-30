package com.saas.MedStorage_api.order.service;

import com.saas.MedStorage_api.order.entity.Order;
import com.saas.MedStorage_api.order.entity.OrderItem;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import com.saas.MedStorage_api.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Falha de envio nao deve impedir a transicao de status do pedido (regra de
 * negocio em docs/specs/04-business-rules.md). Por isso captura e loga em vez
 * de propagar a exececao - quem chama (OrderService) nao precisa saber se o
 * email foi enviado.
 */
@Slf4j
@Service
public class OrderNotificationService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final String fromAddress;
    private final String fromName;

    private static final List<UserRole> STAFF_ROLES = List.of(UserRole.ADMIN, UserRole.GERENTE_ESTOQUE);

    public OrderNotificationService(
            JavaMailSender mailSender,
            UserRepository userRepository,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.mail.from-name}") String fromName) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    public void sendOrderReadyEmail(Order order) {
        String recipient = order.getCustomer().getEmail();
        if (recipient == null || recipient.isBlank()) {
            log.warn("Pedido {} sem email de cliente cadastrado, notificacao nao enviada", order.getNumeroPedido());
            return;
        }
        send(recipient,
                "Pedido " + order.getNumeroPedido() + " esta pronto para retirada",
                buildCustomerBody(order));
    }

    public void sendOrderCreatedToStaff(Order order) {
        List<String> recipients = staffEmails();
        if (recipients.isEmpty()) {
            log.warn("Nenhum admin/gerente ativo encontrado para notificacao de criacao do pedido {}", order.getNumeroPedido());
            return;
        }
        String subject = "Novo pedido registrado: " + order.getNumeroPedido();
        String body = buildOrderCreatedBody(order);
        recipients.forEach(email -> send(email, subject, body));
    }

    public void sendOrderAttendedToStaff(Order order) {
        List<String> recipients = staffEmails();
        if (recipients.isEmpty()) {
            log.warn("Nenhum admin/gerente ativo encontrado para notificacao de atendimento do pedido {}", order.getNumeroPedido());
            return;
        }
        String subject = "Pedido atendido: " + order.getNumeroPedido();
        String body = buildOrderAttendedBody(order);
        recipients.forEach(email -> send(email, subject, body));
    }

    private List<String> staffEmails() {
        return userRepository.findByRoleInAndAtivoTrue(STAFF_ROLES)
                .stream()
                .map(User::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .toList();
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
            log.warn("Falha ao enviar email para {}: {}", recipient, ex.getMessage());
        }
    }

    private String buildCustomerBody(Order order) {
        StringBuilder body = new StringBuilder();
        body.append("Numero do pedido: ").append(order.getNumeroPedido()).append('\n');
        body.append("Data de criacao: ").append(order.getCreatedAt()).append('\n');
        body.append("Pronto em: ").append(order.getDataPronte()).append('\n');
        body.append("Itens:\n");
        for (OrderItem item : order.getItems()) {
            body.append(" - ").append(item.getProduct().getNome())
                    .append(" x").append(item.getQuantidade()).append('\n');
        }
        body.append("Valor total: R$ ").append(order.getValorTotal()).append('\n');
        body.append("Entre em contato com seu vendedor para mais detalhes sobre a retirada.");
        return body.toString();
    }

    private String buildOrderCreatedBody(Order order) {
        StringBuilder body = new StringBuilder();
        body.append("Um novo pedido foi registrado no sistema.\n\n");
        body.append("Pedido: ").append(order.getNumeroPedido()).append('\n');
        body.append("Cliente: ").append(order.getCustomer().getNome()).append('\n');
        body.append("Vendedor: ").append(order.getCriadoPor().getNome()).append('\n');
        body.append("Itens:\n");
        for (OrderItem item : order.getItems()) {
            body.append(" - ").append(item.getProduct().getNome())
                    .append(" x").append(item.getQuantidade())
                    .append(" (R$ ").append(item.getPrecoUnitario()).append(" un.)").append('\n');
        }
        body.append("Valor total: R$ ").append(order.getValorTotal()).append('\n');
        if (order.getNotas() != null && !order.getNotas().isBlank()) {
            body.append("Notas: ").append(order.getNotas()).append('\n');
        }
        return body.toString();
    }

    private String buildOrderAttendedBody(Order order) {
        StringBuilder body = new StringBuilder();
        body.append("O pedido abaixo esta PRONTO para retirada.\n\n");
        body.append("Pedido: ").append(order.getNumeroPedido()).append('\n');
        body.append("Cliente: ").append(order.getCustomer().getNome()).append('\n');
        body.append("Vendedor: ").append(order.getCriadoPor().getNome()).append('\n');
        body.append("Pronto em: ").append(order.getDataPronte()).append('\n');
        body.append("Itens:\n");
        for (OrderItem item : order.getItems()) {
            body.append(" - ").append(item.getProduct().getNome())
                    .append(" x").append(item.getQuantidade()).append('\n');
        }
        body.append("Valor total: R$ ").append(order.getValorTotal()).append('\n');
        return body.toString();
    }
}
