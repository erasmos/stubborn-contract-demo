package com.erasmos.stubborncontractdemo.catalog;

/*
    If we renamed 'title' to something like 'name', at least one contract test would break.

    But if we were to add an extra field (e.g. 'author'), such tests should still pass.
 */
public record Book(Long id, String title) {
}
