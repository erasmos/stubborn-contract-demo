package com.erasmos.stubborncontractdemo.loans;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(RestClient.Builder restClientBuilder, @Value("${catalog.base-url}") String catalogBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(catalogBaseUrl).build();
    }

    public Optional<Book> findBook(Long id) {
        return restClient.get()
                .uri("/books/{id}", id)
                .exchange((request, response) -> {
                    if (response.getStatusCode() == HttpStatus.NOT_FOUND) return Optional.empty();
                    if (response.getStatusCode().isError())
                        throw new IllegalStateException("Catalog returned [%s] for book [%d]".formatted(response.getStatusCode(), id));
                    return Optional.ofNullable(response.bodyTo(Book.class));
                });
    }


}
