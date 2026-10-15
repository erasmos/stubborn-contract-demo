package com.erasmos.stubborncontractdemo.loans;

// The loans service's own view of a catalog book; only the fields it uses.
public record Book(Long id, String title) {
}
