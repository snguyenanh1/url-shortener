package io.github.snguyenanh1.urlshortener.repository;

import io.github.snguyenanh1.urlshortener.entity.Url;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    boolean existsByShortCode(String shortCode);
    Optional<Url> findByShortCode(String shortCode);
    Optional<Url> findByUrl(String url);
}
