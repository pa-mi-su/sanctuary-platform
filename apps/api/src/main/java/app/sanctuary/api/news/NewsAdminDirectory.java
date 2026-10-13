package app.sanctuary.api.news;

import app.sanctuary.api.config.AuthProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

/** Rechecks current membership; a role removed in Cognito cannot use a stale JWT. */
@Service
public class NewsAdminDirectory {
    private final CognitoIdentityProviderClient cognito;
    private final AuthProperties auth;

    public NewsAdminDirectory(@org.springframework.beans.factory.annotation.Qualifier("newsCognitoClient") CognitoIdentityProviderClient cognito, AuthProperties auth) {
        this.cognito = cognito;
        this.auth = auth;
    }

    public boolean active(String username) {
        if (!auth.enabled() || auth.userPoolId() == null || auth.userPoolId().isBlank()) return false;
        try {
            var user = cognito.adminGetUser(AdminGetUserRequest.builder()
                .userPoolId(auth.userPoolId()).username(username).build());
            if (!Boolean.TRUE.equals(user.enabled()) || user.userStatus() != UserStatusType.CONFIRMED
                || !user.userMFASettingList().contains("SOFTWARE_TOKEN_MFA")) return false;
            String next = null;
            do {
                var groups = cognito.adminListGroupsForUser(AdminListGroupsForUserRequest.builder()
                    .userPoolId(auth.userPoolId()).username(username).nextToken(next).limit(60).build());
                if (groups.groups().stream().anyMatch(group -> "news-admin".equals(group.groupName()))) return true;
                next = groups.nextToken();
            } while (next != null && !next.isBlank());
            return false;
        } catch (software.amazon.awssdk.core.exception.SdkException exception) {
            // IAM/network/Cognito failure must never fall back to trusting old membership.
            org.slf4j.LoggerFactory.getLogger(NewsAdminDirectory.class)
                .warn("Administrator membership check failed; access denied");
            return false;
        }
    }
}
