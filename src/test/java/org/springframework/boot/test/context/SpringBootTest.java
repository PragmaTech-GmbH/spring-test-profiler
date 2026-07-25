package org.springframework.boot.test.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test fixture mirroring the real Spring Boot annotation (same FQCN in Boot 3.x and 4.x). The real
 * artifact is not on the test classpath, so detection tests use this stand-in.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface SpringBootTest {}
