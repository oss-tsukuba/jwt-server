package org.oss_tsukuba.utils;

import java.security.Principal;
import java.util.Map;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;

public class KeycloakUtil {

	public static String USER_CLAIM = "hpci.id";
	
	public static String getUserName(Principal principal) {
		String user = null;
		
		if (principal instanceof KeycloakAuthenticationToken) {
			Object obj = ((KeycloakAuthenticationToken) principal).getPrincipal();

			if (obj instanceof KeycloakPrincipal<?>) {
				KeycloakPrincipal<?> keycloakPrincipal = (KeycloakPrincipal<?>) obj;
				AccessToken token = keycloakPrincipal.getKeycloakSecurityContext().getToken();
				Map<String, Object> map = token.getOtherClaims();
				user = (String) map.get(USER_CLAIM);
			}
		}
		
		return user;
	}
}
