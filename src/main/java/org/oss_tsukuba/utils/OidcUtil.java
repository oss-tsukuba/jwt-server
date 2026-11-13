package org.oss_tsukuba.utils;

import java.security.Principal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Map;

import org.oss_tsukuba.oauth2.EnhancedOAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OidcUtil {


    private static Decoder decoder = Base64.getUrlDecoder();

    public static String getUserName(Principal principal, String userClaim) {
        String user = null;

        if (principal instanceof EnhancedOAuth2AuthenticationToken) {
            EnhancedOAuth2AuthenticationToken oauth2token = (EnhancedOAuth2AuthenticationToken) principal;

            if ("".equals(userClaim)) {
                if (oauth2token.getPrincipal() instanceof OidcUser) {
                    user = ((OidcUser) oauth2token.getPrincipal()).getPreferredUsername();
                }
            } else {
                try {
                    String token = oauth2token.getAccessToken().getTokenValue();
                    String[] chunks = token.split("\\.");
                    String json = new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> map = mapper.readValue(json, Map.class);
                    user = (String) map.get(userClaim);
                } catch (Exception e) {
                }
                // Opaque access tokens have no claims to decode. Use the authenticated
                // ID token/UserInfo claims supplied by Spring instead.
                if (user == null) {
                    Object claim = oauth2token.getPrincipal().getAttributes().get(userClaim);
                    if (claim instanceof String) {
                        user = (String) claim;
                    }
                }
            }
        }

        return user;
    }
}
