package org.oss_tsukuba.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class TokenServiceImpl implements TokenService {

	private RestTemplate restTemplate;

	public TokenServiceImpl(RestTemplate restTemplate) {
		super();
		this.restTemplate = restTemplate;
	}

	@Override
	public String getToken(Map<String, String> dummyParams) {
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
			return result.getBody();
		} else {
			return null;
		}
	}
}
