package com.saas.MedStorage_api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(properties = {
        "security.login.rate-limit.max-attempts=1000",
        "jwt.secret=test-secret-key-for-integration-tests-minimum-256-bits-long"
})
@AutoConfigureMockMvc
@Transactional
public @interface IntegrationTest {
}
