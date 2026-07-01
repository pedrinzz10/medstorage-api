package com.saas.MedStorage_api.consignment.service;

import com.saas.MedStorage_api.consignment.dto.CreateVisitRequest;
import com.saas.MedStorage_api.consignment.dto.VisitResponse;
import com.saas.MedStorage_api.consignment.entity.ConsignmentVisit;
import com.saas.MedStorage_api.consignment.enums.VisitStatus;
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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ConsignmentVisitService {

    private final ConsignmentVisitRepository visitRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public ConsignmentVisitService(
            ConsignmentVisitRepository visitRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository) {
        this.visitRepository = visitRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VisitResponse create(CreateVisitRequest request, Authentication authentication) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        User funcionario = userRepository.findById(request.funcionarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário not found"));
        User criadoPor = currentUser(authentication);

        ConsignmentVisit visit = ConsignmentVisit.builder()
                .customer(customer)
                .funcionario(funcionario)
                .dataAgendada(request.dataAgendada())
                .observacoes(request.observacoes())
                .criadoPor(criadoPor)
                .build();

        ConsignmentVisit saved = visitRepository.save(visit);
        log.info("Visita agendada: cliente={} funcionario={} data={}", customer.getNome(), funcionario.getNome(), request.dataAgendada());
        return VisitResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<VisitResponse> findByRange(LocalDate from, LocalDate to) {
        return visitRepository.findByDataAgendadaBetween(from, to).stream()
                .sorted(Comparator.comparing(ConsignmentVisit::getDataAgendada))
                .map(VisitResponse::from)
                .toList();
    }

    @Transactional
    public VisitResponse cancel(UUID id) {
        ConsignmentVisit visit = getOrThrow(id);
        if (visit.getStatus() == VisitStatus.REALIZADA) {
            throw new BadRequestException("Visita já realizada não pode ser cancelada");
        }
        visit.setStatus(VisitStatus.CANCELADA);
        ConsignmentVisit saved = visitRepository.save(visit);
        log.info("Visita {} cancelada", id);
        return VisitResponse.from(saved);
    }

    /** Chamado ao registrar a contagem vinculada a esta visita (Fatia C). */
    @Transactional
    public void markRealizada(UUID id) {
        ConsignmentVisit visit = getOrThrow(id);
        if (visit.getStatus() == VisitStatus.CANCELADA) {
            throw new BadRequestException("Visita cancelada não pode ser marcada como realizada");
        }
        visit.setStatus(VisitStatus.REALIZADA);
        visitRepository.save(visit);
    }

    private ConsignmentVisit getOrThrow(UUID id) {
        return visitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
