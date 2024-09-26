package org.oss_tsukuba.service;

import static org.oss_tsukuba.dao.Error.*;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.oss_tsukuba.dao.Error;
import org.oss_tsukuba.dao.ErrorRepository;
import org.oss_tsukuba.dao.Token;
import org.oss_tsukuba.dao.TokenRepository;
import org.oss_tsukuba.oauth2.EnhancedOAuth2AuthenticationToken;
import org.oss_tsukuba.utils.CryptUtil;
import org.oss_tsukuba.utils.Damm;
import org.oss_tsukuba.utils.KeycloakUtil;
import org.oss_tsukuba.utils.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class TokenServiceImpl implements TokenService {

    private RestTemplate restTemplate;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    ErrorRepository errorRepository;

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String baseUrl;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String secret;

    @Value("${user-claim:}")
    private String userClaim;

    private static String EXP_CLAIM = "exp";

    static private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public TokenServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    private String getExpDate(String token) {
        String[] jwts = token.split("\\.");
        String date = "";

        if (jwts.length == 3) {
            String json = new String(Base64.getDecoder().decode(jwts[1]));

            try {
                JSONObject jsonObj = (JSONObject) new JSONParser().parse(json);

                Long exp = (Long) jsonObj.get(EXP_CLAIM);
                Instant instant = Instant.ofEpochSecond(exp);

                return df.format(Date.from(instant));
            } catch (ParseException e) {
                LogUtils.error(e.toString(), e);
            }

        }

        return date;
    }

    @Override
    public void getToken(Principal principal, Model model) {
        String jwt = null;

        if (principal instanceof EnhancedOAuth2AuthenticationToken) {
            EnhancedOAuth2AuthenticationToken oauth2token = (EnhancedOAuth2AuthenticationToken) principal;

            String user = KeycloakUtil.getUserName(principal, userClaim);
            String rToken = oauth2token.getRefreshToken().getTokenValue();

            String url = baseUrl + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", clientId);
            params.add("refresh_token", rToken);
            params.add("client_secret", secret);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(params,
                    headers);

            try {
                ResponseEntity<String> result = restTemplate.postForEntity(url, request, String.class);

                HttpStatusCode responseHttpStatus = result.getStatusCode();

                if (responseHttpStatus.equals(HttpStatus.OK)) { // 200
                    jwt = result.getBody();
                } else {
                    model.addAttribute("error", 2);
                    Error error = new Error(user, "", "jwt-server", UNEXPECTED_ERROR);
                    errorRepository.save(error);

                    LogUtils.info(String.format("User error occured(%s, %s, %s)", error.getUser(), error.getError(),
                            error.getIpAddr()));
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
                    model.addAttribute("user", user);
                    model.addAttribute("token", accessToken);

                    String expDate = getExpDate(accessToken);
                    model.addAttribute("exp", expDate);

                    try {
                        byte[] iv = CryptUtil.generateIV();
                        byte[] enc1 = CryptUtil.encrypt(accessToken.getBytes(), key, iv);
                        byte[] enc2 = CryptUtil.encrypt(refreshToken.getBytes(), key, iv);

                        tokenRepository.save(new Token(user, clientId, Base64.getEncoder().encodeToString(enc1),
                                Base64.getEncoder().encodeToString(enc2), Base64.getEncoder().encodeToString(iv)));

                        LogUtils.info(String.format("User:%s changed passphrase from Web Browser", user));
                    } catch (Exception e) {
                        LogUtils.error(e.toString(), e);
                    }
                }
            } catch (RestClientException e) {
                model.addAttribute("error", 1);
                Error error = new Error(user, "", "jwt-server", SERVER_DOWN);
                errorRepository.save(error);

                LogUtils.info(String.format("User error occured(%s, %s, %s)", error.getUser(), error.getError(),
                        error.getIpAddr()));
            }
        }
    }

    @Override
    public String getToken(String user, String pass) {
        String jwt = null;

        String url = baseUrl + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", "password");
        params.add("client_id", clientId);
        params.add("username", user);
        params.add("password", pass);
        params.add("client_secret", secret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(params,
                headers);

        ResponseEntity<String> result = new RestTemplate().postForEntity(url, request, String.class);

        HttpStatusCode responseHttpStatus = result.getStatusCode();

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

            try {
                byte[] iv = CryptUtil.generateIV();
                byte[] enc1 = CryptUtil.encrypt(accessToken.getBytes(), key, iv);
                byte[] enc2 = CryptUtil.encrypt(refreshToken.getBytes(), key, iv);

                tokenRepository.save(new Token(user, clientId, Base64.getEncoder().encodeToString(enc1),
                        Base64.getEncoder().encodeToString(enc2), Base64.getEncoder().encodeToString(iv)));
            } catch (Exception e) {
                LogUtils.error(e.toString(), e);
            }

            return passphrase;
        }

        return null;
    }
}
