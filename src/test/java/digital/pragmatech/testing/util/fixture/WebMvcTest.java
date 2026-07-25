package digital.pragmatech.testing.util.fixture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Decoy annotation with a known slice simple name but outside the {@code org.springframework.}
 * package. Must NOT be detected.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface WebMvcTest {}
