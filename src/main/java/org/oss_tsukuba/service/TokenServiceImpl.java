package org.oss_tsukuba.service;

import java.security.Principal;
import java.util.Base64;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.oss_tsukuba.dao.Token;
import org.oss_tsukuba.dao.TokenRepository;
import org.oss_tsukuba.utils.CryptUtil;
import org.oss_tsukuba.utils.Damm;
import org.oss_tsukuba.utils.KeycloakUtil;
import org.oss_tsukuba.utils.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class TokenServiceImpl implements TokenService {

	private RestTemplate restTemplate;

	@Autowired
	private TokenRepository tokenRepository;

	@Value("${keycloak.auth-server-url}")
	private String baseUrl;
	
	@Value("${keycloak.realm}")
	private String realm;
	
	@Value("${keycloak.resource}")
	private String clientId;
	
	@Value("${keycloak.credentials.secret}")
	private String secret;
	
	public TokenServiceImpl(RestTemplate restTemplate) {
		super();
		this.restTemplate = restTemplate;
	}

	@Override
	public void getToken(Principal principal, Model model) {
		String jwt = null;

		if (principal instanceof KeycloakAuthenticationToken) {
			Object obj = ((KeycloakAuthenticationToken) principal).getPrincipal();

			if (obj instanceof KeycloakPrincipal<?>) {
				KeycloakPrincipal<?> keycloakPrincipal = (KeycloakPrincipal<?>) obj;
				String user = KeycloakUtil.getUserName(principal);
				String rToken = ((RefreshableKeycloakSecurityContext)keycloakPrincipal.getKeycloakSecurityContext()).getRefreshToken();

				String url = baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
				params.add("grant_type", "refresh_token");
				params.add("client_id", clientId);
				params.add("refresh_token", rToken);
				params.add("client_secret", secret);
				HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(
						params, headers);

				ResponseEntity<String> result = restTemplate.postForEntity(url, request, String.class);

				HttpStatus responseHttpStatus = result.getStatusCode();

				if (responseHttpStatus.equals(HttpStatus.OK)) { // 200
					jwt = result.getBody();
				}

				if (jwt != null) {
					LogUtils.trace(jwt);
					
					String accessToken = null;
					String refreshToken = null;
					
					try {
						JSONObject js = (JSONObject) new JSONParser().parse(jwt);
						accessToken = (String) js.get("access_token");
						refreshToken = (String) js.get("refresh_token");
					} catch (ParseException e) {
						LogUtils.error(e.toString(), e);
					}

					Damm dmm = new Damm();

					String key = dmm.getPassphrase();
					String passphrase = key + dmm.damm32Encode(key.toCharArray());

					model.addAttribute("passphrase", passphrase);

					try {
						byte[] iv = CryptUtil.generateIV();
						byte[] enc1 = CryptUtil.encrypt(accessToken.getBytes(), key, iv);
						byte[] enc2 = CryptUtil.encrypt(refreshToken.getBytes(), key, iv);

						tokenRepository.save(new Token(user, clientId, Base64.getEncoder().encodeToString(enc1),
								Base64.getEncoder().encodeToString(enc2),
								Base64.getEncoder().encodeToString(iv)));
					} catch (Exception e) {
						LogUtils.error(e.toString(), e);
					}
				}
			}
		}
	}
}
