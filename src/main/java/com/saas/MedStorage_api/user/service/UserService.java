package com.saas.MedStorage_api.user.service;

import com.saas.MedStorage_api.auth.dto.UserSummaryResponse;
import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.user.dto.UpdateUserRequest;
import com.saas.MedStorage_api.user.dto.UserResponse;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import com.saas.MedStorage_api.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private static final List<UserRole> STAFF_ROLES = List.of(UserRole.ADMIN, UserRole.GERENTE_ESTOQUE);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    /**
     * Lista enxuta de ADMIN/GERENTE_ESTOQUE ativos, para atribuição de
     * funcionário em visitas de consignação — não expõe o cadastro completo
     * de usuários, que é restrito a ADMIN via findAll().
     */
    public List<UserSummaryResponse> findStaff() {
        return userRepository.findByRoleInAndAtivoTrue(STAFF_ROLES).stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    public UserResponse findById(UUID id) {
        return UserResponse.from(getOrThrow(id));
    }

    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = getOrThrow(id);
        UserRole role;
        try {
            role = UserRole.fromValue(request.role());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role: " + request.role());
        }
        user.setNome(request.nome());
        user.setTelefone(request.telefone());
        user.setRole(role);
        user.setAtivo(request.ativo());
        User saved = userRepository.save(user);
        log.info("Usuário {} atualizado: role={} ativo={}", saved.getEmail(), saved.getRole(), saved.isAtivo());
        return UserResponse.from(saved);
    }

    private User getOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
