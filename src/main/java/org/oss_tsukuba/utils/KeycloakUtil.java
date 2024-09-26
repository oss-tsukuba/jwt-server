package org.oss_tsukuba.utils;

import java.security.Principal;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Map;

import org.oss_tsukuba.oauth2.EnhancedOAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import com.fasterxml.jackson.databind.ObjectMapper;

public class KeycloakUtil {


    private static Decoder decoder = Base64.getUrlDecoder();

    public static String getUserName(Principal principal, String userClaim) {
        String user = null;

        if (principal instanceof EnhancedOAuth2AuthenticationToken) {
            EnhancedOAuth2AuthenticationToken oauth2token = (EnhancedOAuth2AuthenticationToken) principal;

            if ("".equals(userClaim)) {
                user = ((DefaultOidcUser) oauth2token.getPrincipal()).getIdToken().getPreferredUsername();
            } else {
                try {
                    String token = oauth2token.getAccessToken().getTokenValue();
                    String[] chunks = token.split("\\.");
                    String json = new String(decoder.decode(chunks[1]));
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> map = mapper.readValue(json, Map.class);
                    user = (String) map.get(userClaim);
                } catch (Exception e) {
                }
            }
        }

        return user;
    }
}
