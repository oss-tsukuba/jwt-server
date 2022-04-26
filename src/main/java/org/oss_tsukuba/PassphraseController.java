package org.oss_tsukuba;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.oss_tsukuba.dao.Error;
import org.oss_tsukuba.dao.ErrorRepository;
import org.oss_tsukuba.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@Controller
public class PassphraseController {

	@Autowired
	private TokenService tokenService;
	
	@Autowired
	private ErrorRepository errorRepository;

	@GetMapping(path = "/index")
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
	
	@GetMapping(path = "/error")
	public String getError(Model model) {

		return "error";
	}
}
