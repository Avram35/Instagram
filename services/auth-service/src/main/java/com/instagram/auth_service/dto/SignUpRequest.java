package com.instagram.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {

    @NotBlank(message = "Корисничко име је обавезно.")
    private String username;

    @NotBlank(message = "Email је обавезан.")
    @Email(message = "Email није исправан.")
    private String email;

    @NotBlank(message = "Име је обавезно.")
    private String fname;

    @NotBlank(message = "Презиме је обавезно.")
    private String lname;

    @NotBlank(message = "Шифра је обавезна.")
    private String password;

}

