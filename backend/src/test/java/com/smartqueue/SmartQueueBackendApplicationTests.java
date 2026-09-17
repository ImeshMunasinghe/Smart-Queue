package com.smartqueue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(MockRedisConfig.class)
class SmartQueueBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
