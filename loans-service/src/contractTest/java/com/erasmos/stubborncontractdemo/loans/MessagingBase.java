package com.erasmos.stubborncontractdemo.loans;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

import java.util.Optional;

import static org.mockito.Mockito.when;

/*
  Every generated contract test extends this. Unlike catalog's, it starts the whole app,
  because the message has to go through the real KafkaTemplate to an (embedded) broker.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(topics = "book-checked-out")
@AutoConfigureMessageVerifier // lets the generated test read the message back from the topic
@DisplayNameGeneration(ContractDisplayNames.class)
public abstract class MessagingBase {

    @Autowired
    LoansService loansService;

    @MockitoBean
    CatalogClient mockCatalogClient; // no catalog running here; its behaviour is covered by its own contracts

    // Called by the contract's triggeredBy.
    public void checkOutBook() {
        when(mockCatalogClient.findBook(100L)).thenReturn(Optional.of(new Book(100L, "Dune")));
        loansService.checkOut(100L, 7L);
    }

}
