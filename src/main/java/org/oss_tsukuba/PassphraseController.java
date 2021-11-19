package org.oss_tsukuba;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.oss_tsukuba.dao.Error;
import org.oss_tsukuba.dao.ErrorRepository;
import org.oss_tsukuba.dao.Token;
import org.oss_tsukuba.dao.TokenRepository;
import org.oss_tsukuba.service.TokenService;
import org.oss_tsukuba.utils.CryptUtil;
import org.oss_tsukuba.utils.Damm;
import org.oss_tsukuba.utils.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.security.Principal;
import java.util.Base64;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@Controller
public class PassphraseController {

	@Autowired
	private TokenService tokenService;
	
	@Autowired
	private ErrorRepository errorRepository;

	@GetMapping(path = "/")
	public String getIndex(Model model) {

		return "index";
	}

	@GetMapping(path = "/passphrase")
	public String getPassphrase(Principal principal, Model model) {
		tokenService.getToken(principal, model);
		
		return "passphrase";
	}

	@GetMapping(path = "/errors")
	public String getErrros(Principal principal, Model model, Pageable pageable) {
		if (principal instanceof KeycloakAuthenticationToken) {
			Object obj = ((KeycloakAuthenticationToken) principal).getPrincipal();

			if (obj instanceof KeycloakPrincipal<?>) {
				KeycloakPrincipal<?> keycloakPrincipal = (KeycloakPrincipal<?>) obj;
				String user = keycloakPrincipal.getKeycloakSecurityContext().getIdToken().getPreferredUsername();
				Page<Error> errors = errorRepository.findByUserOrderByIdDesc(pageable, user);
		        model.addAttribute("page", errors);
		        model.addAttribute("errors", errors.getContent());
		        model.addAttribute("url", "errors");
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
