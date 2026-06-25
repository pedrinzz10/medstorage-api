package com.saas.MedStorage_api.notification;

import com.saas.MedStorage_api.domain.order.Order;
import com.saas.MedStorage_api.domain.order.OrderItem;
import io.mailtrap.client.MailtrapClient;
import io.mailtrap.model.request.emails.Address;
import io.mailtrap.model.request.emails.MailtrapMail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Falha de envio nao deve impedir a transicao de status do pedido (regra de
 * negocio em docs/specs/04-business-rules.md). Por isso captura e loga em vez
 * de propagar a exececao - quem chama (OrderService) nao precisa saber se o
 * email foi enviado.
 */
@Service
public class OrderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationService.class);

    private final MailtrapClient mailtrapClient;
    private final String fromAddress;
    private final String fromName;

    public OrderNotificationService(
            MailtrapClient mailtrapClient,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.mail.from-name}") String fromName) {
        this.mailtrapClient = mailtrapClient;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    public void sendOrderReadyEmail(Order order) {
        String recipient = order.getCustomer().getEmail();
        if (recipient == null || recipient.isBlank()) {
            log.warn("Pedido {} sem email de cliente cadastrado, notificacao nao enviada", order.getNumeroPedido());
            return;
        }

        MailtrapMail mail = MailtrapMail.builder()
                .from(new Address(fromAddress, fromName))
                .to(List.of(new Address(recipient)))
                .subject("Pedido " + order.getNumeroPedido() + " esta pronto para retirada")
                .text(buildBody(order))
                .category("Pedido pronto para retirada")
                .build();

        try {
            mailtrapClient.send(mail);
        } catch (Exception ex) {
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
