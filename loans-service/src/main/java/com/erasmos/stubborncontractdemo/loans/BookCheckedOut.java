package com.erasmos.stubborncontractdemo.loans;

import java.time.Instant;
import java.time.LocalDate;

public record BookCheckedOut(Long loanId, Long bookId, Long memberId, Instant checkedOutAt, LocalDate dueDate) {

    // The new name for memberId. Both are sent until no consumer reads memberId (expand and contract).
    public Long getBorrowerId(){
        return memberId;
    }

}
