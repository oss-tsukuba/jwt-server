package org.oss_tsukuba.oauth2;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;

public class Oauth2ResultConverter implements Converter<OAuth2LoginAuthenticationToken, OAuth2AuthenticationToken> {

    public OAuth2AuthenticationToken convert(OAuth2LoginAuthenticationToken authenticationResult) {
        return new EnhancedOAuth2AuthenticationToken(authenticationResult.getPrincipal(), authenticationResult.getAuthorities(),
                authenticationResult.getClientRegistration().getRegistrationId(),
                authenticationResult.getAccessToken(), authenticationResult.getRefreshToken());
    }
}
