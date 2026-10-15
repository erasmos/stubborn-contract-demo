package com.erasmos.stubborncontractdemo.catalog;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class BookCatalog {

    private final Map<Long, Book> books = Map.of(
            200L, new Book(200L, "Dune"),
            400L, new Book(400L, "Neuromancer")
    );

    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(books.get(id));
    }

}
