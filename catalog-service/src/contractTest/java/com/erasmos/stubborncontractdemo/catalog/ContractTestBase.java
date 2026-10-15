package com.erasmos.stubborncontractdemo.catalog;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Every generated contract test extends this; it decides what the contract is verified against.
@DisplayNameGeneration(ContractTestBase.ContractDisplayNames.class)
public abstract class ContractTestBase {

    @BeforeEach
    void setUp() {
        // The data each contract relies on, stated here rather than borrowed from BookCatalog's seed data.
        BookCatalog mockBookCatalog = mock(BookCatalog.class);
        when(mockBookCatalog.findById(100L)).thenReturn(Optional.of(new Book(100L, "Dune")));
        when(mockBookCatalog.findById(999L)).thenReturn(Optional.empty()); // Not needed, as it's the default.
        RestAssuredMockMvc.standaloneSetup(new BookController(mockBookCatalog));
    }

    // Generated test methods can't carry @DisplayName, so derive one from the method name:
    // validate_returns_the_book_when_it_exists -> "returns the book when it exists."
    static class ContractDisplayNames extends DisplayNameGenerator.Standard {
        @Override
        public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass, Method testMethod) {
            String name = testMethod.getName().replaceFirst("^validate_", "").replace('_', ' ');
            return name + ".";
        }
    }

}

