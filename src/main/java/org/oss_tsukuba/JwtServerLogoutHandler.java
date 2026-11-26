package org.oss_tsukuba;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import org.oss_tsukuba.dao.TokenTime;
import org.oss_tsukuba.dao.TokenTimeRepository;
import org.oss_tsukuba.utils.OidcUtil;
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

        String user = OidcUtil.getUserName(authentication, userClaim);

        super.onLogoutSuccess(request, response, authentication);

        // save logout time, only for a user who has generated a token
        if (user != null) {
            Optional<TokenTime> opt = tokenTimeRepository.findById(user);

            if (opt.isPresent()) {
                TokenTime tokenTime = opt.get();
                tokenTime.setLogoutAt(Instant.now().getEpochSecond());
                tokenTimeRepository.save(tokenTime);
            }
        }
    }

}
