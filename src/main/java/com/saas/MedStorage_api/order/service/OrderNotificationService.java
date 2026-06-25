package com.saas.MedStorage_api.order.service;

import com.saas.MedStorage_api.order.entity.Order;
import com.saas.MedStorage_api.order.entity.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Falha de envio nao deve impedir a transicao de status do pedido (regra de
 * negocio em docs/specs/04-business-rules.md). Por isso captura e loga em vez
 * de propagar a exececao - quem chama (OrderService) nao precisa saber se o
 * email foi enviado.
 */
@Service
public class OrderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public OrderNotificationService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.mail.from-name}") String fromName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    public void sendOrderReadyEmail(Order order) {
        String recipient = order.getCustomer().getEmail();
        if (recipient == null || recipient.isBlank()) {
            log.warn("Pedido {} sem email de cliente cadastrado, notificacao nao enviada", order.getNumeroPedido());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromAddress + ">");
            message.setTo(recipient);
            message.setSubject("Pedido " + order.getNumeroPedido() + " esta pronto para retirada");
            message.setText(buildBody(order));
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Falha ao enviar email de notificacao do pedido {}: {}", order.getNumeroPedido(), ex.getMessage());
        }
    }

    private String buildBody(Order order) {
        StringBuilder body = new StringBuilder();
        body.append("Numero do pedido: ").append(order.getNumeroPedido()).append('\n');
        body.append("Data de criacao: ").append(order.getCreatedAt()).append('\n');
        body.append("Data de atendimento: ").append(order.getDataAtendimento()).append('\n');
        body.append("Itens:\n");
        for (OrderItem item : order.getItems()) {
            body.append(" - ").append(item.getProduct().getNome())
                    .append(" x").append(item.getQuantidade())
                    .append('\n');
        }
        body.append("Valor total: R$ ").append(order.getValorTotal()).append('\n');
        body.append("Entre em contato com seu vendedor para mais detalhes sobre a retirada.");
        return body.toString();
    }
}
