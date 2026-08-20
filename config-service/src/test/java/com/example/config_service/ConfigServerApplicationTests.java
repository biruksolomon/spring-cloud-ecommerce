package com.example.config_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        // "native" is a built-in Spring Cloud Config profile that swaps the
        // git-backed EnvironmentRepository for a classpath/filesystem one, so
        // the test context never shells out to git or needs config-repo/.
        "spring.profiles.active=native",
        "spring.cloud.config.server.native.search-locations=classpath:/config-test-repo",
        "eureka.client.enabled=false"
})
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
    }

}