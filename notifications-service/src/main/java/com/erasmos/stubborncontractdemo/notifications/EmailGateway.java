package com.erasmos.stubborncontractdemo.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailGateway {

    private static final Logger logger = LoggerFactory.getLogger(EmailGateway.class);

    public void send(Long memberId, String message) {
        logger.info("Emailing member [{}]: {}", memberId, message);
    }

}
