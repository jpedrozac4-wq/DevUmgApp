package gt.com.ro.devumgapp.auth.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ProfileErrorPolicyTest {
    @Test public void classifiesRequiredBackendErrors() {
        assertEquals(ProfileErrorPolicy.Kind.INVALID_DATA, ProfileErrorPolicy.classify(400, false));
        assertEquals(ProfileErrorPolicy.Kind.EXPIRED_SESSION, ProfileErrorPolicy.classify(401, false));
        assertEquals(ProfileErrorPolicy.Kind.CURRENT_PASSWORD, ProfileErrorPolicy.classify(401, true));
        assertEquals(ProfileErrorPolicy.Kind.DUPLICATE, ProfileErrorPolicy.classify(409, false));
        assertEquals(ProfileErrorPolicy.Kind.RATE_LIMIT, ProfileErrorPolicy.classify(429, true));
    }
}
