package com.saas.MedStorage_api.consignment.service;

import com.saas.MedStorage_api.consignment.dto.ConsignmentCountResponse;
import com.saas.MedStorage_api.consignment.dto.CountItemRequest;
import com.saas.MedStorage_api.consignment.dto.RegisterCountRequest;
import com.saas.MedStorage_api.consignment.entity.ConsignmentCount;
import com.saas.MedStorage_api.consignment.entity.ConsignmentCountItem;
import com.saas.MedStorage_api.consignment.entity.ConsignmentItem;
import com.saas.MedStorage_api.consignment.entity.ConsignmentVisit;
import com.saas.MedStorage_api.consignment.repository.ConsignmentCountRepository;
import com.saas.MedStorage_api.consignment.repository.ConsignmentItemRepository;
import com.saas.MedStorage_api.consignment.repository.ConsignmentVisitRepository;
import com.saas.MedStorage_api.customer.entity.Customer;
import com.saas.MedStorage_api.customer.repository.CustomerRepository;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ConsignmentCountService {

    private final ConsignmentCountRepository consignmentCountRepository;
    private final ConsignmentItemRepository consignmentItemRepository;
    private final ConsignmentVisitRepository consignmentVisitRepository;
    private final ConsignmentVisitService consignmentVisitService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public ConsignmentCountService(
            ConsignmentCountRepository consignmentCountRepository,
            ConsignmentItemRepository consignmentItemRepository,
            ConsignmentVisitRepository consignmentVisitRepository,
            ConsignmentVisitService consignmentVisitService,
            CustomerRepository customerRepository,
            UserRepository userRepository) {
        this.consignmentCountRepository = consignmentCountRepository;
        this.consignmentItemRepository = consignmentItemRepository;
        this.consignmentVisitRepository = consignmentVisitRepository;
        this.consignmentVisitService = consignmentVisitService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ConsignmentCountResponse registerCount(RegisterCountRequest request, Authentication authentication) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        User funcionario = currentUser(authentication);

        ConsignmentVisit visit = null;
        if (request.visitId() != null) {
            visit = consignmentVisitRepository.findById(request.visitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        }

        ConsignmentCount count = ConsignmentCount.builder()
                .customer(customer)
                .visit(visit)
                .funcionario(funcionario)
                .dataContagem(request.dataContagem())
                .build();

        for (CountItemRequest itemRequest : request.items()) {
            ConsignmentItem item = consignmentItemRepository.findById(itemRequest.consignmentItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Consignment item not found: " + itemRequest.consignmentItemId()));
            if (!item.getConsignment().getCustomer().getId().equals(customer.getId())) {
                throw new BadRequestException("Item não pertence a este cliente");
            }

            int saldoEsperado = item.getSaldoDisponivel();
            int divergencia = saldoEsperado - itemRequest.quantidadeContada();

            count.addItem(ConsignmentCountItem.builder()
                    .consignmentItem(item)
                    .quantidadeContada(itemRequest.quantidadeContada())
                    .loteConferido(itemRequest.loteConferido())
                    .validadeConferida(itemRequest.validadeConferida())
                    .divergencia(divergencia)
                    .build());
        }

        ConsignmentCount saved = consignmentCountRepository.save(count);

        if (request.visitId() != null) {
            consignmentVisitService.markRealizada(request.visitId());
        }

        log.info("Contagem registrada: cliente={} itens={} funcionario={}",
                customer.getNome(), request.items().size(), funcionario.getEmail());
        return ConsignmentCountResponse.from(saved);
    }

    /**
     * @param customerId opcional — quando nulo, retorna o histórico completo
     *                    de todos os clientes (mais recente primeiro).
     */
    @Transactional(readOnly = true)
    public List<ConsignmentCountResponse> findByCustomer(UUID customerId) {
        List<ConsignmentCount> counts = customerId != null
                ? consignmentCountRepository.findByCustomer_IdOrderByDataContagemDesc(customerId)
                : consignmentCountRepository.findAllByOrderByDataContagemDesc();
        return counts.stream()
                .map(ConsignmentCountResponse::from)
                .toList();
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
