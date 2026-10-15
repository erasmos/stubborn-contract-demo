package com.erasmos.stubborncontractdemo.loans;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LoansService {

    private static final String BOOK_CHECKED_OUT_TOPIC = "book-checked-out";

    private final CatalogClient catalogClient;
    private final KafkaTemplate<String, BookCheckedOut> kafkaTemplate;
    private final Clock clock;

    /*
        Would normally be a database sequence.
     */
    private final AtomicLong nextLoanId = new AtomicLong(1);

    public LoansService(CatalogClient catalogClient, KafkaTemplate<String, BookCheckedOut> kafkaTemplate, Clock clock) {
        this.catalogClient = catalogClient;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    public BookCheckedOut checkOut(Long bookId, Long memberId) {
        if (catalogClient.findBook(bookId).isEmpty()) throw new UnknownBookException(bookId);
        Instant now = clock.instant();
        BookCheckedOut bookCheckedOut = new BookCheckedOut(
                nextLoanId.getAndIncrement(),
                bookId,
                memberId,
                now,
                LocalDate.ofInstant(now, clock.getZone()).plusDays(30));
        kafkaTemplate.send(BOOK_CHECKED_OUT_TOPIC, bookCheckedOut);
        return bookCheckedOut;
    }
}
