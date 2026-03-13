package com.instagram.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignInRequest {

    @NotBlank(message = "Корисничко име или email је обавезан.")
    private String usernameOrEmail;

    @NotBlank(message = "Шифра је обавезна.")
    private String password;

}

