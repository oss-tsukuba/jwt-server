package org.oss_tsukuba;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.oss_tsukuba.dao.Error;
import org.oss_tsukuba.dao.ErrorRepository;
import org.oss_tsukuba.dao.Passphrase;
import org.oss_tsukuba.dao.PassphraseRepository;
import org.oss_tsukuba.utils.CryptUtil;
import org.oss_tsukuba.utils.Damm;
import org.oss_tsukuba.utils.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Base64;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@Controller
public class PassphraseController {

	private RestTemplate restTemplate;

	@Autowired
	PassphraseRepository passphraseRepository;

	@Autowired
	ErrorRepository errorRepository;

	public PassphraseController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@GetMapping(path = "/")
	public String getIndex(Model model) {

		return "index";
	}

	@GetMapping(path = "/passphrase")
	public String getPassphrase(Principal principal, Model model) {
		String url = "http://can3.canaly.co.jp:8080/auth/realms/gfarm_service/protocol/openid-connect/token";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("grant_type", "client_credentials");
		params.add("client_secret", "8fe5eeb6-8cc5-478a-9aad-397a1cd621b2");
		params.add("client_id", "jwt-saver");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(params,
				headers);

		ResponseEntity<String> result = restTemplate.postForEntity(url, request, String.class);

		HttpStatus responseHttpStatus = result.getStatusCode();

		if (responseHttpStatus.equals(HttpStatus.OK)) { // 200
			String jwt = result.getBody();
			LogUtils.trace(jwt);

			Damm dmm = new Damm();

			String key = dmm.getPassphrase();
			String passphrase = key + dmm.damm32Encode(key.toCharArray());

			model.addAttribute("passphrase", passphrase);

			if (principal instanceof KeycloakAuthenticationToken) {
				Object obj = ((KeycloakAuthenticationToken) principal).getPrincipal();

				if (obj instanceof KeycloakPrincipal<?>) {
					KeycloakPrincipal<?> keycloakPrincipal = (KeycloakPrincipal<?>) obj;
					String user = keycloakPrincipal.getKeycloakSecurityContext().getIdToken().getPreferredUsername();

					try {
						byte[] iv = CryptUtil.generateIV();
						byte[] enc = CryptUtil.encrypt(jwt.getBytes(), key, iv);
						passphraseRepository.save(new Passphrase(user, Base64.getEncoder().encodeToString(enc), Base64.getEncoder().encodeToString(iv)));
					} catch (Exception e) {
						LogUtils.error(e.toString(), e);
					}
				}

			}

		} else if (responseHttpStatus.equals(HttpStatus.BAD_REQUEST)) {
			// ステータスコード400の場合

		} else if (responseHttpStatus.equals(HttpStatus.UNAUTHORIZED)) {
			// ステータスコード401の場合
		}

		return "passphrase";
	}

	@GetMapping(path = "/errors")
	public String getErrros(Principal principal, Model model) {
		if (principal instanceof KeycloakAuthenticationToken) {
			Object obj = ((KeycloakAuthenticationToken) principal).getPrincipal();

			if (obj instanceof KeycloakPrincipal<?>) {
				KeycloakPrincipal<?> keycloakPrincipal = (KeycloakPrincipal<?>) obj;
				String user = keycloakPrincipal.getKeycloakSecurityContext().getIdToken().getPreferredUsername();
				List<Error> errors = errorRepository.findByUserOrderByIdDesc(user);
				model.addAttribute("errors", errors);
			}
		}

		return "errors";
	}

	@GetMapping(path = "/logout")
	public String logout(HttpServletRequest request) throws ServletException {
		request.logout();
		return "redirect:/";
	}
}
