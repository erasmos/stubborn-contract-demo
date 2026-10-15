package com.erasmos.stubborncontractdemo.loans;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoansService loansService;

    public LoanController(LoansService loansService) {
        this.loansService = loansService;
    }

    @PostMapping
    ResponseEntity<CheckOutResponse> checkOut(@RequestBody CheckOutRequest checkOutRequest) {
        BookCheckedOut bookCheckedOut = loansService.checkOut(checkOutRequest.bookId(), checkOutRequest.memberId());
        return ResponseEntity.created(URI.create("/loans/" + bookCheckedOut.loanId()))
                .body(new CheckOutResponse(bookCheckedOut.loanId()));
    }

    // The request is well-formed but refers to a book that doesn't exist.
    @ExceptionHandler(UnknownBookException.class)
    ResponseEntity<Void> unknownBook() {
        return ResponseEntity.unprocessableContent().build();
    }

}
