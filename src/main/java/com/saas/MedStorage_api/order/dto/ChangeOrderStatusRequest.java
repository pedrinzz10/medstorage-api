package com.saas.MedStorage_api.order.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeOrderStatusRequest(@NotBlank String newStatus) {
}
