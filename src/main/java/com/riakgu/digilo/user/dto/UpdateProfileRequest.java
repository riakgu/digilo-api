package com.riakgu.digilo.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Pattern(regexp = "^(\\+62|62|0)[0-9]{9,13}$", message = "Invalid phone number format")
    private String phone;

}
