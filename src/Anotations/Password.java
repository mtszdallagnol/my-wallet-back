package Anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Password {
    String message() default "A senha deve conter pelo menos uma letra maiúscula, uma letra minúscula, um número e um caractere especial";
}