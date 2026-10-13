package app.sanctuary.api.news;

import java.util.List;
import org.junit.jupiter.api.Test;
import app.sanctuary.api.config.AuthProperties;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class NewsAdminDirectoryTest {
    @Test void revokedDisabledAndUnavailableAccountsCannotContinueUsingOldSessions() {
        var cognito=mock(CognitoIdentityProviderClient.class);
        var directory=new NewsAdminDirectory(cognito,new AuthProperties(true,"client","pool","client"));
        when(cognito.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(AdminGetUserResponse.builder().enabled(true).userStatus(UserStatusType.CONFIRMED).userMFASettingList("SOFTWARE_TOKEN_MFA").build());
        when(cognito.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class)))
            .thenReturn(AdminListGroupsForUserResponse.builder().groups(GroupType.builder().groupName("news-admin").build()).build())
            .thenReturn(AdminListGroupsForUserResponse.builder().groups(List.of()).build());
        assertThat(directory.active("admin")).isTrue();
        assertThat(directory.active("admin")).isFalse();
        when(cognito.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(AdminGetUserResponse.builder().enabled(true).userStatus(UserStatusType.CONFIRMED).build());
        assertThat(directory.active("admin")).isFalse();
        when(cognito.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(AdminGetUserResponse.builder().enabled(false).build());
        assertThat(directory.active("admin")).isFalse();
        when(cognito.adminGetUser(any(AdminGetUserRequest.class))).thenThrow(NotAuthorizedException.builder().build());
        assertThat(directory.active("admin")).isFalse();
    }
}
