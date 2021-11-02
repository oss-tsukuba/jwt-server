package org.oss_tsukuba;

import java.util.Base64;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.oss_tsukuba.dao.Error;
import org.oss_tsukuba.dao.ErrorRepository;
import org.oss_tsukuba.dao.Passphrase;
import org.oss_tsukuba.dao.PassphraseRepository;
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

	@Autowired
	PassphraseRepository passphraseRepository;

	@Autowired
	ErrorRepository errorRepository;

	@RequestMapping(value = "/jwt", method = RequestMethod.POST)
	public String getJwt(@RequestParam(name = "user", required = true) String user,
			@RequestParam(name = "pass", required = true) String pass, HttpServletRequest request) {
		String ipAddr = request.getRemoteAddr();
		String hostname = request.getRemoteHost();
		
		String jwt = "";

		Damm damm = new Damm();
		char[] code = pass.toCharArray();
		
		// 文字数の検査
		if (!damm.isValidLength(code)) {
			// error
			LogUtils.error("length error");
			Error error = new Error(user, ipAddr, hostname, LENGTH_ERROR);
			errorRepository.save(error);

			return null;
		}
		
		// 文字の検査
		if (!damm.isValidChar(code)) {
			// error
			LogUtils.error("character error");
			Error error = new Error(user, ipAddr, hostname, CHARACTER_ERROR);
			errorRepository.save(error);

			return null;
		}
		
		// check digit の検査
		if (!damm.damm32Check(code)) {
			// error
			LogUtils.error("check digit error");
			Error error = new Error(user, ipAddr, hostname, CHECK_DIGIT_ERROR);
			errorRepository.save(error);

			return null;
		}

		// 復号化
		try {
			String key = pass.substring(0, pass.length() - 1);
			Optional<Passphrase> optional = passphraseRepository.findById(user);
			Passphrase passphrase = optional.get();
			byte[] enc = Base64.getDecoder().decode(passphrase.getPhrase());
			byte[] jwtByte = CryptUtil.decrypt(enc, key, Base64.getDecoder().decode(passphrase.getIv()));
			jwt = new String(jwtByte);
		} catch (Exception e) {
			LogUtils.error(e.toString(), e);
			Error error = new Error(user, ipAddr, hostname, DECRYPT_ERROR);
			errorRepository.save(error);

			return null;
		}

		return jwt;
	}
}
