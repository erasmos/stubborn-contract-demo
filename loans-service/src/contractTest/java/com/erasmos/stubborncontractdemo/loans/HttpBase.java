package com.erasmos.stubborncontractdemo.loans;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(ContractDisplayNames.class)
public abstract class HttpBase {

    @BeforeEach
    void setUp() {
        LoansService mockLoansService = mock(LoansService.class);
        when(mockLoansService.checkOut(100L, 7L)).thenReturn(
                new BookCheckedOut(1L, 100L, 7L, Instant.parse("2026-10-15T10:00:00Z"), LocalDate.parse("2026-11-05")));
        when(mockLoansService.checkOut(999L, 7L)).thenThrow(new UnknownBookException(999L));
        RestAssuredMockMvc.standaloneSetup(new LoanController(mockLoansService));
    }

}
