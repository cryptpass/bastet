package com.arpanrec.bastet.test;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class ApplicationTests {
    private final Logger logger = LoggerFactory.getLogger(ApplicationTests.class);

    @Test
    void testApplication() {
        logger.info("testApplication");
    }
}