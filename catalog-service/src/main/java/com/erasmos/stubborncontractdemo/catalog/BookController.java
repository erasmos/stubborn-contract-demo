package com.erasmos.stubborncontractdemo.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookCatalog bookCatalog;

    public BookController(BookCatalog bookCatalog) {
        this.bookCatalog = bookCatalog;
    }

    @GetMapping("/{id}")
    ResponseEntity<Book> findById(@PathVariable Long id) {
        return ResponseEntity.of(bookCatalog.findById(id));
    }

}
