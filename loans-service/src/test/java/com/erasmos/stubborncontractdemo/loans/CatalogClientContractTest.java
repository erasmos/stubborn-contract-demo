package com.erasmos.stubborncontractdemo.loans;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "catalog.base-url=http://localhost:8090")
/*
    Reads the stubs from the local 'catalog-service' and serves it with WireMock on port 8090.
    The format of ids is "group:artifact:version:classifier:port". The '+' for the version means the latest.
 */

@AutoConfigureStubRunner(ids = "com.erasmos:catalog-service:+:stubs:8090", stubsMode = StubsMode.CLASSPATH)
@DisplayName("The catalog should")
public class CatalogClientContractTest {

    @Autowired
    private CatalogClient catalogClient;

    @AfterEach
    void everyRequestMatchedAStub() {
        // WireMock answers unmatched requests with 404, which can look like a real "not found";
        // fail instead, so every response the client sees comes from a contract.
        WireMock catalogStubs = new WireMock(8090);
        try {
            assertThat(catalogStubs.findAllUnmatchedRequests()).as("Requests that matched no catalog stub").isEmpty();
        } finally {
            catalogStubs.resetRequests(); // the request log is shared by all tests in the class
        }
    }

    @Test
    @DisplayName("find a book that exists in the catalog.")
    void findsBookThatExists() {
        assertThat(catalogClient.findBook(100L)).contains(new Book(100L, "Dune"));
    }

    @Test
    @DisplayName("find nothing for a book that isn't in the catalog.")
    void findsNothingForUnknownBook() {
        assertThat(catalogClient.findBook(999L)).isEmpty();
    }

    @Test
    @DisplayName("find any book with a numeric id (except 999, which is considered unknown).")
    void findsAnyBookWithNumericId() {
        assertThat(catalogClient.findBook(101L)).contains(new Book(101L, "Dune"));
    }
}
