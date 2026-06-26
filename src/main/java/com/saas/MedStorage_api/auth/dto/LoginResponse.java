package com.saas.MedStorage_api.auth.dto;

public record LoginResponse(String token, UserSummaryResponse user) {
}
