package org.oss_tsukuba;

import static org.oss_tsukuba.dao.Error.*;

import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.oss_tsukuba.dao.Error;
import org.oss_tsukuba.dao.ErrorRepository;
import org.oss_tsukuba.dao.Token;
import org.oss_tsukuba.dao.TokenKey;
import org.oss_tsukuba.dao.TokenRepository;
import org.oss_tsukuba.service.TokenService;
import org.oss_tsukuba.utils.CryptUtil;
import org.oss_tsukuba.utils.Damm;
import org.oss_tsukuba.utils.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
public class JwtController {

    private RestTemplate restTemplate;

    class ErrorInfo {
        long time;
        int count;
    }

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    ErrorRepository errorRepository;

    @Autowired
    private TokenService tokenService;

    @Value("${keycloak.auth-server-url}")
    private String baseUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String secret;

    @Value("${jwt-server.passphrase:}")
    private String initialPassphrase;

    private boolean initPassphrase;

    private Map<String, ErrorInfo> errorMap;

    private long expireTime = 1000 * 60 * 60; // 1時間

    public JwtController() {
        errorMap = new ConcurrentHashMap<String, ErrorInfo>();
        this.restTemplate = new RestTemplate();
        this.initPassphrase = false;
    }

    private int getIntervalTime(int count) {
        return count < 4? count: 4;
    }

    private String error(String user) {

        ErrorInfo ei = errorMap.get(user);
        long now = new Date().getTime();

        if (ei == null || now - ei.time > expireTime) {
            ei = new ErrorInfo();
            errorMap.put(user, ei);
        }

        ei.time = now;
        ei.count++;

        int interval = getIntervalTime(ei.count);

        if (interval > 0) {
            try {
                Thread.sleep(interval * 1000);
            } catch (InterruptedException e) {
            }
        }

        return null;
    }

    private String getJwt(String refreshToken) {
        String jwt = null;

        String url = baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", clientId);
        params.add("refresh_token", refreshToken);
        params.add("client_secret", secret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(
                params, headers);

        try {
            ResponseEntity<String> result = restTemplate.postForEntity(url, request, String.class);

            HttpStatus responseHttpStatus = result.getStatusCode();

            if (responseHttpStatus.equals(HttpStatus.OK)) { // 200
                jwt = result.getBody();
            }
        } catch (HttpClientErrorException e) {
            LogUtils.error(e.toString(), e);
        }

        return jwt;
    }

    private String checkPassphrase(String user, String ipAddr, String hostname, char[] code) {
        Damm damm = new Damm();

        // 文字数の検査
        if (!damm.isValidLength(code)) {
            // error
            LogUtils.error("length error");
            Error error = new Error(user, ipAddr, hostname, LENGTH_ERROR);
            errorRepository.save(error);

            return error(user);
        }

        // 文字の検査
        if (!damm.isValidChar(code)) {
            // error
            LogUtils.error("character error");
            Error error = new Error(user, ipAddr, hostname, CHARACTER_ERROR);
            errorRepository.save(error);

            return error(user);
        }

        // check digit の検査
        if (!damm.damm32Check(code)) {
            // error
            LogUtils.error("check digit error");
            Error error = new Error(user, ipAddr, hostname, CHECK_DIGIT_ERROR);
            errorRepository.save(error);

            return error(user);
        }

        return "ok";
    }

    @RequestMapping(value = "/init_jwt", method = RequestMethod.POST)
    public String initJwt(@RequestParam(name = "passphrase", required = true) String passphrase,
            @RequestParam(name = "user", required = true) String user,
            @RequestParam(name = "password", required = true) String password, HttpServletRequest request) {
        String ipAddr = request.getRemoteAddr();
        String hostname = request.getRemoteHost();

        if (initPassphrase || "".equals(initialPassphrase)) {
            return null;
        }

        char[] code = passphrase.toCharArray();

        String ok = checkPassphrase(user, ipAddr, hostname, code);

        if (ok == null) {
            return null;
        }

        if (passphrase.equals(initialPassphrase)) {
            String newPassphrase = tokenService.getToken(user, password);

            if (newPassphrase != null) {

                initPassphrase = true;

                return newPassphrase;
            }
        }

        return null;
    }

    @RequestMapping(value = "/chpass", method = RequestMethod.POST)
    public String changePassphrase(@RequestParam(name = "user", required = true) String user,
            @RequestParam(name = "pass", required = true) String pass, HttpServletRequest request) {
        String ipAddr = request.getRemoteAddr();
        String hostname = request.getRemoteHost();

        if (hostname.equals(ipAddr)) {
            hostname = "";
        }

        char[] code = pass.toCharArray();

        String ok = checkPassphrase(user, ipAddr, hostname, code);

        if (ok == null) {
            return null;
        }

        String newPassphrase = null;

        // decrypt
        try {
            String key = pass.substring(0, pass.length() - 1);
            Optional<Token> optional = tokenRepository.findById(new TokenKey(user, clientId));
            Token passphrase = optional.get();
            byte[] iv = Base64.getDecoder().decode(passphrase.getIv());

            // refresh token
            byte[] encRefresh = Base64.getDecoder().decode(passphrase.getRefreshToken());
            byte[] byteRefresh = CryptUtil.decrypt(encRefresh, key, iv);

            // access token
            byte[] encAccess = Base64.getDecoder().decode(passphrase.getAccessToken());
            byte[] byteAccess = CryptUtil.decrypt(encAccess, key, iv);


            Damm dmm = new Damm();

            String newKey = dmm.getPassphrase();
            newPassphrase = newKey + dmm.damm32Encode(newKey.toCharArray());

            byte[] enc1 = CryptUtil.encrypt(byteAccess, newKey, iv);
            byte[] enc2 = CryptUtil.encrypt(byteRefresh, newKey, iv);

            tokenRepository.save(new Token(user, clientId, Base64.getEncoder().encodeToString(enc1),
                    Base64.getEncoder().encodeToString(enc2),
                    Base64.getEncoder().encodeToString(iv)));

        } catch (Exception e) {
            LogUtils.error(e.toString(), e);
            Error error = new Error(user, ipAddr, hostname, DECRYPT_ERROR);
            errorRepository.save(error);

            return error(user);
        }

        // Auth Success
        if (errorMap.containsKey(user)) {
            errorMap.remove(user);
        }

        return newPassphrase;
    }

    @RequestMapping(value = "/jwt", method = RequestMethod.POST)
    public String getJwt(@RequestParam(name = "user", required = true) String user,
            @RequestParam(name = "pass", required = true) String pass, HttpServletRequest request) {
        String ipAddr = request.getRemoteAddr();
        String hostname = request.getRemoteHost();

        if (hostname.equals(ipAddr)) {
            // 名前が引けない場合は空文字列にする。
            hostname = "";
        }

        String accessToken = null;

        char[] code = pass.toCharArray();

        String ok = checkPassphrase(user, ipAddr, hostname, code);

        if (ok == null) {
            return null;
        }

        // decrypt
        try {
            String key = pass.substring(0, pass.length() - 1);
            Optional<Token> optional = tokenRepository.findById(new TokenKey(user, clientId));
            Token passphrase = optional.get();
            byte[] iv = Base64.getDecoder().decode(passphrase.getIv());

            // refresh token
            byte[] encRefresh = Base64.getDecoder().decode(passphrase.getRefreshToken());
            byte[] byteRefresh = CryptUtil.decrypt(encRefresh, key, iv);
            String refreshToken = new String(byteRefresh);

            // get JWT
            String jwt = getJwt(refreshToken);

            if (jwt != null) {
                try {
                    JSONObject js = (JSONObject) new JSONParser().parse(jwt);
                    accessToken = (String) js.get("access_token");
                    refreshToken = (String) js.get("refresh_token");
                } catch (ParseException e) {
                    LogUtils.error(e.toString(), e);
                }

                try {
                    byte[] enc1 = CryptUtil.encrypt(accessToken.getBytes(), key, iv);
                    byte[] enc2 = CryptUtil.encrypt(refreshToken.getBytes(), key, iv);

                    passphrase.setAccessToken(Base64.getEncoder().encodeToString(enc1));
                    passphrase.setRefreshToken(Base64.getEncoder().encodeToString(enc2));

                    tokenRepository.save(passphrase);
                } catch (Exception e) {
                    LogUtils.error(e.toString(), e);
                }
            } else {
                Error error = new Error(user, ipAddr, hostname, EXPIRED_ERROR);
                errorRepository.save(error);

                return error(user);
            }
        } catch (Exception e) {
            LogUtils.error(e.toString(), e);
            Error error = new Error(user, ipAddr, hostname, DECRYPT_ERROR);
            errorRepository.save(error);

            return error(user);
        }

        // Auth Success
        if (errorMap.containsKey(user)) {
            errorMap.remove(user);
        }

        return accessToken;
    }
}
