package com.erasmos.stubborncontractdemo.notifications;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookCheckedOutListener {

    private final EmailGateway emailGateway;

    public BookCheckedOutListener(EmailGateway emailGateway) {
        this.emailGateway = emailGateway;
    }

    @KafkaListener(topics = "book-checked-out")
    void on(BookCheckedOut bookCheckedOut) {
        emailGateway.send(bookCheckedOut.borrowerId(), "You checked out book [%d].".formatted(bookCheckedOut.bookId()));
    }
}
