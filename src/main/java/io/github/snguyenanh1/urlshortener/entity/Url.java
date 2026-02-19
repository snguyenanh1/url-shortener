package io.github.snguyenanh1.urlshortener.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

@Entity
@Data
@Table(name = "url")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @URL(message = "Invalid URL")
    @NotBlank(message = "URL cannot be blank")
    @Column(nullable = false)
    private String url;

    @Column(nullable = false, unique = true)
    private String shortCode;

    @NotNull(message = "Creation date cannot be null")
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Future(message = "Expiration date must be in the future")
    @NotNull(message = "Expiration date must not be null")
    @Column(nullable = false)
    private OffsetDateTime expiresAt;

}
