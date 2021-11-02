package org.oss_tsukuba;

import java.util.Base64;
import java.util.Date;
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

@RestController
public class JwtController {

	@Autowired
	PassphraseRepository passphraseRepository;

	@Autowired
	ErrorRepository errorRepository;

	@RequestMapping(value = "/jwt", method = RequestMethod.POST)
	public String getJwt(@RequestParam(name = "user", required = true) String user,
			@RequestParam(name = "pass", required = true) String pass, HttpServletRequest request) {
		String jwt = "";

		// check digit の検査
		Damm dmm = new Damm();
		if (!dmm.damm32Check(pass.toCharArray())) {
			// error
			LogUtils.error("check digit error");
			Error error = new Error();
			error.setUser(user);
			error.setDate(new Date());
			error.setType(0);
			errorRepository.save(error);

			return "error no: 0";
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
			Error error = new Error();
			error.setUser(user);
			error.setDate(new Date());
			error.setType(1);
			errorRepository.save(error);

			return "error no: 1";
		}

		return jwt;
	}
}