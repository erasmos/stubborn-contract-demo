package com.erasmos.stubborncontractdemo.loans;

public class UnknownBookException extends RuntimeException{

    public UnknownBookException(Long bookId) {
        super("Book [%d] doesn't exist".formatted(bookId));
    }

}
