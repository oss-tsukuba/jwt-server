package org.oss_tsukuba;

import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.oss_tsukuba.dao.Error;
import org.oss_tsukuba.dao.ErrorRepository;
import org.oss_tsukuba.dao.Token;
import org.oss_tsukuba.dao.TokenRepository;
import org.oss_tsukuba.utils.CryptUtil;
import org.oss_tsukuba.utils.Damm;
import org.oss_tsukuba.utils.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.oss_tsukuba.dao.Error.CHECK_DIGIT_ERROR;
import static org.oss_tsukuba.dao.Error.DECRYPT_ERROR;
import static org.oss_tsukuba.dao.Error.LENGTH_ERROR;
import static org.oss_tsukuba.dao.Error.CHARACTER_ERROR;;

@RestController
public class JwtController {

	class ErrorInfo {
		long time;
		int count;
	}
	
	@Autowired
	TokenRepository passphraseRepository;

	@Autowired
	ErrorRepository errorRepository;
	
	private Map<String, ErrorInfo> errorMap;

	private long expireTime = 1000 * 60 * 60; // 1時間
	
	public JwtController() {
		errorMap = new ConcurrentHashMap<String, ErrorInfo>();
	}
	
	private int getIntervalTime(int count) {
		return (count - 1) * (count - 1);
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
	
	@RequestMapping(value = "/jwt", method = RequestMethod.POST)
	public String getJwt(@RequestParam(name = "user", required = true) String user,
			@RequestParam(name = "pass", required = true) String pass, HttpServletRequest request) {
		String ipAddr = request.getRemoteAddr();
		String hostname = request.getRemoteHost();
		
		if (hostname.equals(ipAddr)) {
			// 名前が引けない場合は空文字列にする。
			hostname = "";
		}
		
		String jwt = "";

		Damm damm = new Damm();
		char[] code = pass.toCharArray();
		
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

		// 復号化
		try {
			String key = pass.substring(0, pass.length() - 1);
			Optional<Token> optional = passphraseRepository.findById(user);
			Token passphrase = optional.get();
			byte[] enc = Base64.getDecoder().decode(passphrase.getToken());
			byte[] jwtByte = CryptUtil.decrypt(enc, key, Base64.getDecoder().decode(passphrase.getIv()));
			jwt = new String(jwtByte);
		} catch (Exception e) {
			LogUtils.error(e.toString(), e);
			Error error = new Error(user, ipAddr, hostname, DECRYPT_ERROR);
			errorRepository.save(error);

			return error(user);
		}

		// 認証成功で連続を削除
		if (errorMap.containsKey(user)) {
			errorMap.remove(user);
		}
		
		return jwt;
	}
}
