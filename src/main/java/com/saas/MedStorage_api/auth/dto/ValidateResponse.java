package com.saas.MedStorage_api.auth.dto;

public record ValidateResponse(boolean valid, String email, String role) {
}
