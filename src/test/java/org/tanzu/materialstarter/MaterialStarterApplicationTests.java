package org.tanzu.materialstarter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "claude-code.enabled=false"
})
class MaterialStarterApplicationTests {

    @Test
    void contextLoads() {
    }

}
