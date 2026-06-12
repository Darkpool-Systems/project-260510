package com.back;

import com.back.global.config.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestContainersConfig.class)
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
