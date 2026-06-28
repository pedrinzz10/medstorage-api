package com.saas.MedStorage_api.user;

import com.saas.MedStorage_api.exception.BadRequestException;
import com.saas.MedStorage_api.exception.ResourceNotFoundException;
import com.saas.MedStorage_api.user.dto.UpdateUserRequest;
import com.saas.MedStorage_api.user.dto.UserResponse;
import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import com.saas.MedStorage_api.user.repository.UserRepository;
import com.saas.MedStorage_api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User vendedor;

    @BeforeEach
    void setUp() {
        vendedor = User.builder()
                .id(UUID.randomUUID())
                .email("vendedor1@distribuidor.com")
                .passwordHash("hash")
                .nome("João Vendedor")
                .role(UserRole.VENDEDOR)
                .ativo(true)
                .telefone("11987654321")
                .build();
    }

    @Test
    void findAll_returnsPagedUsers() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(vendedor), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<UserResponse> result = userService.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("vendedor1@distribuidor.com", result.getContent().get(0).email());
    }

    @Test
    void findById_withExistingId_returnsUser() {
        when(userRepository.findById(vendedor.getId())).thenReturn(Optional.of(vendedor));

        UserResponse response = userService.findById(vendedor.getId());

        assertEquals("João Vendedor", response.nome());
        assertEquals("vendedor", response.role());
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(unknownId));
    }

    @Test
    void update_withValidRequest_updatesFields() {
        UpdateUserRequest request = new UpdateUserRequest("João Renomeado", "11911112222", "gerente_estoque", true);
        when(userRepository.findById(vendedor.getId())).thenReturn(Optional.of(vendedor));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.update(vendedor.getId(), request);

        assertEquals("João Renomeado", response.nome());
        assertEquals("gerente_estoque", response.role());
        assertEquals("11911112222", response.telefone());
    }

    @Test
    void update_deactivatesUser() {
        UpdateUserRequest request = new UpdateUserRequest("João Vendedor", null, "vendedor", false);
        when(userRepository.findById(vendedor.getId())).thenReturn(Optional.of(vendedor));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.update(vendedor.getId(), request);

        assertFalse(response.ativo());
    }

    @Test
    void update_withInvalidRole_throwsBadRequestException() {
        UpdateUserRequest request = new UpdateUserRequest("João", null, "diretor", true);
        when(userRepository.findById(vendedor.getId())).thenReturn(Optional.of(vendedor));

        assertThrows(BadRequestException.class, () -> userService.update(vendedor.getId(), request));
    }

    @Test
    void update_withUnknownId_throwsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest("Nome", null, "vendedor", true);
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.update(unknownId, request));
    }
}
