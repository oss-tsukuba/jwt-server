package org.oss_tsukuba.service;

import java.util.Map;

public interface TokenService {

	String getToken(Map<String, String> params);
}
