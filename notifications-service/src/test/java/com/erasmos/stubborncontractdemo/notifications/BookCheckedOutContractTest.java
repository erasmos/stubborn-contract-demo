package com.erasmos.stubborncontractdemo.notifications;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import sh.stubborn.contract.stubrunner.StubTrigger;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(topics = "book-checked-out")
// No port, unlike CatalogClientContractTest: messaging stubs send to Kafka rather than serve HTTP.
@AutoConfigureStubRunner(ids = "com.erasmos:loans-service:+:stubs", stubsMode = StubsMode.CLASSPATH)
@DisplayName("The notifications service when")
public class BookCheckedOutContractTest {

    @Autowired
    StubTrigger stubTrigger;

    @MockitoBean
    EmailGateway mockEmailGateway;

    @Test
    @DisplayName("a book is checked out, the member is emailed.")
    void emailsTheMemberWhenTheyCheckOutABook() {
        // Sends the contract's example message (borrowerId 7, bookId 100) to book-checked-out.
        // book_checked_out is the contract's label (see shouldPublishBookCheckedOut.yml).
        stubTrigger.trigger("book_checked_out");
        // The listener runs on another thread, so wait for it.
        Mockito.verify(mockEmailGateway, Mockito.timeout(10_000)).send(7L, "You checked out book [100].");
    }

}
