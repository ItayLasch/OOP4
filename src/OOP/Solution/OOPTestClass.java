package OOP.Solution;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OOPTestClass {
    public enum OOPTestClassType {
        ORDERED, UNORDERED
    };

    OOPTestClassType value() default OOPTestClassType.UNORDERED;
}
