package app.sanctuary.api.news;

import java.security.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.*;
import app.sanctuary.api.config.*;
import static org.assertj.core.api.Assertions.*;

/** Uses actual RSA signatures and the production validator, without network discovery. */
class NewsSignedTokenSecurityTest {
    static KeyPair pair;
    NimbusJwtDecoder decoder;
    AuthProperties auth = new AuthProperties(true,"web-client","pool","web-client");

    @BeforeAll static void keys() throws Exception {
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);pair=generator.generateKeyPair();
    }
    @BeforeEach void setup() {
        decoder=NimbusJwtDecoder.withPublicKey((RSAPublicKey)pair.getPublic()).build();
        decoder.setJwtValidator(CognitoTokenValidators.forClient("https://issuer.example.org/pool","web-client"));
    }
    String token(String issuer,String client,String use,Instant expiry,KeyPair signer) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer(issuer).subject("administrator")
            .issueTime(Date.from(Instant.now().minusSeconds(10))).expirationTime(Date.from(expiry))
            .claim("token_use",use).claim("cognito:groups",List.of("news-admin"));
        if("access".equals(use))claims.claim("client_id",client);else claims.audience(client);
        var signed=new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),claims.build());
        signed.sign(new RSASSASigner((RSAPrivateKey)signer.getPrivate()));return signed.serialize();
    }
    @Test void acceptsOnlyValidSignedIdTokenForAdmin()throws Exception {
        assertThat(NewsIdentityPolicy.accepts(decoder.decode(token("https://issuer.example.org/pool","web-client","id",Instant.now().plusSeconds(300),pair)),auth)).isTrue();
        var access=decoder.decode(token("https://issuer.example.org/pool","web-client","access",Instant.now().plusSeconds(300),pair));
        assertThat(NewsIdentityPolicy.accepts(access,auth)).isFalse();
    }
    @Test void nativeConsumerIdAndAccessTokensRemainValidWithoutAdminGroups()throws Exception {
        for(String use:List.of("id","access")) {
            var claims=new JWTClaimsSet.Builder().issuer("https://issuer.example.org/pool").subject("ordinary-mobile-user")
                .issueTime(Date.from(Instant.now().minusSeconds(10))).expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("token_use",use);
            if("id".equals(use))claims.audience("web-client");else claims.claim("client_id","web-client");
            var signed=new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),claims.build());
            signed.sign(new RSASSASigner((RSAPrivateKey)pair.getPrivate()));
            var identity=decoder.decode(signed.serialize());
            assertThat(identity.getSubject()).isEqualTo("ordinary-mobile-user");
            assertThat(NewsIdentityPolicy.accepts(identity,auth)).isFalse();
        }
    }
    @Test void rejectsWrongClientIssuerExpiredAndWrongSignature()throws Exception {
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);var other=generator.generateKeyPair();
        for(String token:List.of(
            token("https://issuer.example.org/pool","other-client","id",Instant.now().plusSeconds(300),pair),
            token("https://evil.example.org/pool","web-client","id",Instant.now().plusSeconds(300),pair),
            token("https://issuer.example.org/pool","web-client","id",Instant.now().minusSeconds(120),pair),
            token("https://issuer.example.org/pool","web-client","refresh",Instant.now().plusSeconds(300),pair),
            token("https://issuer.example.org/pool","web-client","id",Instant.now().plusSeconds(300),other),
            "not.a.token")) assertThatThrownBy(()->decoder.decode(token)).isInstanceOf(JwtException.class);
    }
    @Test void missingClientFailsClosed() {
        assertThatThrownBy(()->CognitoTokenValidators.forClient("https://issuer.example.org", ""))
            .isInstanceOf(IllegalStateException.class);
    }
}
