package com.futslot.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import com.futslot.TestcontainersConfiguration;

/**
 * Full application against a real PostgreSQL. Every class using this shares one Spring context
 * (and one database container), so tests must not depend on the database being empty.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, TestSupportConfig.class })
public @interface IntegrationTest {

}
