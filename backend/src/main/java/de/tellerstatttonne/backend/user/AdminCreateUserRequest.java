package de.tellerstatttonne.backend.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AdminCreateUserRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotEmpty Set<String> roleNames,
    String phone,
    String street,
    String postalCode,
    String city,
    String country
) {}
