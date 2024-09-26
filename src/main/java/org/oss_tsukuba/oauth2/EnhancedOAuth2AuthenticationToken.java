package org.oss_tsukuba.oauth2;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class EnhancedOAuth2AuthenticationToken extends OAuth2AuthenticationToken {

    private static final long serialVersionUID = 1L;

    private OAuth2AccessToken accessToken;

    private OAuth2RefreshToken refreshToken;

    public EnhancedOAuth2AuthenticationToken(OAuth2User principal, Collection<? extends GrantedAuthority> authorities,
            String authorizedClientRegistrationId, OAuth2AccessToken accessToken, OAuth2RefreshToken refreshToken) {
        super(principal, authorities, authorizedClientRegistrationId);
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public OAuth2AccessToken getAccessToken() {
        return accessToken;
    }

    public OAuth2RefreshToken getRefreshToken() {
        return refreshToken;
    }
}
