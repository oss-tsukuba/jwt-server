package org.oss_tsukuba.service;

import java.security.Principal;

import org.springframework.ui.Model;

public interface TokenService {

    void getToken(Principal principal, Model model);

    String getToken(String user, String pass);
}
