package com.nev.common.core.validate.encoding;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects strings that already contain Unicode replacement characters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Constraint(validatedBy = NoReplacementCharValidator.class)
public @interface NoReplacementChar {

    String message() default "不能包含非法替换字符 U+FFFD(EFBFBD)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
