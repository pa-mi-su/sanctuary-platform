package app.sanctuary.api.user.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public record CurrentUser(
    String cognitoSub,
    String email,
    String firstName,
    String lastName,
    String displayName,
    String avatarUrl
) {
    public static CurrentUser from(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Authenticated Cognito JWT is required.");
        }

        String displayName = firstPresent(
            jwt.getClaimAsString("name"),
            joinNames(jwt.getClaimAsString("given_name"), jwt.getClaimAsString("family_name")),
            jwt.getClaimAsString("preferred_username"),
            jwt.getClaimAsString("cognito:username"),
            jwt.getSubject()
        );

        return new CurrentUser(
            jwt.getSubject(),
            jwt.getClaimAsString("email"),
            jwt.getClaimAsString("given_name"),
            jwt.getClaimAsString("family_name"),
            displayName,
            firstPresent(
                jwt.getClaimAsString("picture"),
                jwt.getClaimAsString("custom:avatar_url")
            )
        );
    }

    private static String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String joinNames(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return null;
        }

        if (firstName == null || firstName.isBlank()) {
            return lastName;
        }

        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }

        return firstName + " " + lastName;
    }
}
