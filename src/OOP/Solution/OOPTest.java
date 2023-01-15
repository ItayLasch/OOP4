package OOP.Solution;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OOPTest {
    int order() default 1;
    String tag() default "";
}
