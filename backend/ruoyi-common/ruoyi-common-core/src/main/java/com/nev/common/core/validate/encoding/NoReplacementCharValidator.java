package com.nev.common.core.validate.encoding;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * U+FFFD is the decoded form of UTF-8 bytes EFBFBD, usually meaning corrupted input.
 */
public class NoReplacementCharValidator implements ConstraintValidator<NoReplacementChar, CharSequence> {

    private static final char REPLACEMENT_CHAR = '\uFFFD';

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == REPLACEMENT_CHAR) {
                return false;
            }
        }
        return true;
    }
}
