package com.erasmos.stubborncontractdemo.notifications;

import java.time.Instant;

public record BookCheckedOut(Long loanId, Long bookId, Long borrowerId, Instant checkedOutAt) {
}
