package com.instagram.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {

    @NotBlank(message = "Корисничко име је обавезно!")
    @Size(min = 1, max = 30, message = "Корисничко име мора бити између 1 и 30 карактера!")
    @Pattern(regexp = "^(?=.*[a-zA-Z])[\\w.]+$", message = "Корисничко име може садржати само слова, цифре, тачке и доње црте!")
    private String username;

    @NotBlank(message = "Email је обавезан!")
    @Email(message = "Email није исправан!")
    @Size(max = 50, message = "Email не може бити дужи од 50 карактера!")
    private String email;

    @NotBlank(message = "Име је обавезно!")
    @Size(min = 2, max = 20, message = "Име мора бити између 2 и 20 слова!")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Име може садржати само слова!")
    private String fname;

    @NotBlank(message = "Презиме је обавезно!")
    @Size(min = 2, max = 20, message = "Презиме мора бити између 2 и 20 слова!")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Презиме може садржати само слова!")
    private String lname;

    @NotBlank(message = "Шифра је обавезна!")
    @Size(min = 6, max = 100, message = "Шифра мора имати најмање 6 карактера!")
    @Pattern
    (
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~!@#$%^&*()_+{}\\[\\]:;|/,.\\-=])[\\w~!@#$%^&*()_+{}\\[\\]:;|/,.\\-=]+$",
            message = "Шифра мора садржати најмање једно мало слово, једно велико слово, једну цифру и један специјални карактер!"
    )

    private String password;

}

