package gt.com.ro.devumgapp.auth.ui;

import static org.junit.Assert.*;
import org.junit.Test;

public class ProfileValidatorTest {
    @Test public void passwordAcceptsInclusiveBoundaries() {
        assertEquals(ProfileValidator.PasswordError.NONE, ProfileValidator.validatePassword("old-pass", "12345678"));
        assertEquals(ProfileValidator.PasswordError.NONE, ProfileValidator.validatePassword("old-pass", repeat('x', 72)));
    }

    @Test public void passwordRejectsOutsideBoundariesAndSameValue() {
        assertEquals(ProfileValidator.PasswordError.INVALID_LENGTH, ProfileValidator.validatePassword("old-pass", "1234567"));
        assertEquals(ProfileValidator.PasswordError.INVALID_LENGTH, ProfileValidator.validatePassword("old-pass", repeat('x', 73)));
        assertEquals(ProfileValidator.PasswordError.SAME_PASSWORD, ProfileValidator.validatePassword("same-password", "same-password"));
    }

    @Test public void profileRequiresAllFieldsAndEmailShape() {
        assertTrue(ProfileValidator.isProfileValid("ana", "ana@example.com", "Ana", "López"));
        assertFalse(ProfileValidator.isProfileValid("ana", "invalid", "Ana", "López"));
        assertFalse(ProfileValidator.isProfileValid("", "ana@example.com", "Ana", "López"));
    }

    private static String repeat(char value, int count) { StringBuilder result = new StringBuilder(); for (int i = 0; i < count; i++) result.append(value); return result.toString(); }
}
