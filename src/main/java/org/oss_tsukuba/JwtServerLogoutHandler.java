package org.oss_tsukuba;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import org.oss_tsukuba.dao.TokenTime;
import org.oss_tsukuba.dao.TokenTimeRepository;
import org.oss_tsukuba.utils.KeycloakUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtServerLogoutHandler extends OidcClientInitiatedLogoutSuccessHandler{

    private String userClaim;

    private TokenTimeRepository tokenTimeRepository;

    public JwtServerLogoutHandler(ClientRegistrationRepository clientRegistrationRepository,
            TokenTimeRepository tokenTimeRepository, String userClaim) {
        super(clientRegistrationRepository);
        this.userClaim = userClaim;
        this.tokenTimeRepository = tokenTimeRepository;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        String user = KeycloakUtil.getUserName(authentication, userClaim);

        super.onLogoutSuccess(request, response, authentication);

        // save logout time;
        Optional<TokenTime> opt = tokenTimeRepository.findById(user);
        TokenTime tokenTime = null;

        if (opt.isPresent()) {
            tokenTime = opt.get();
        } else {
            tokenTime = new TokenTime();
        }

        tokenTime.setLogoutAt(Instant.now().getEpochSecond());

        tokenTimeRepository.save(tokenTime);
    }

}
