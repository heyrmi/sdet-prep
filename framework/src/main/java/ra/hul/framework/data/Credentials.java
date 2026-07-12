package ra.hul.framework.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immutable-ish value object holding a generated set of login credentials.
 * Not tied to any real account — produced by {@link CredentialFactory} for test data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Credentials {
    private String username;
    private String password;
    private String email;
}
