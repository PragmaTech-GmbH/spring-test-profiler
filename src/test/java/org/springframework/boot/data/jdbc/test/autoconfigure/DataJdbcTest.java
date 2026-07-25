package org.springframework.boot.data.jdbc.test.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Test fixture mirroring the relocated Spring Boot 4.x slice annotation FQCN. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DataJdbcTest {}
