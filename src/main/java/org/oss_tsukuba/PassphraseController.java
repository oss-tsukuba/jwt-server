package org.oss_tsukuba;

import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.oss_tsukuba.dao.Error;
import org.oss_tsukuba.dao.ErrorRepository;
import org.oss_tsukuba.dao.Issue;
import org.oss_tsukuba.dao.IssueRepository;
import org.oss_tsukuba.service.TokenService;
import org.oss_tsukuba.utils.KeycloakUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jp.co.canaly.mcp.util.PropUtils;

@Controller
public class PassphraseController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ErrorRepository errorRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Value("${user-claim:}")
    private String userClaim;

    @Value("${replicated-jwt-servers:}")
    private String replicatedServers;

    private String version = PropUtils.getValue("version");

    private DateFormat formatter = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

    @GetMapping(path = "/")
    public String getRoot(Model model) {

        return "redirect:menu";
    }

    @GetMapping(path = "/index")
    public String getIndex(Model model) {

        return "redirect:menu";
    }

    @GetMapping(path = "/menu")
    public String getMenu(Model model) {

        model.addAttribute("version", version);

        return "menu";
    }

    @PostMapping(path = "/passphrase")
    public String getPassphrase(Principal principal, Model model, HttpServletRequest request) {
        model.addAttribute("error", 0);
        tokenService.getToken(principal, model);

        String uri = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
        uri = uri.replace("passphrase", "");
        model.addAttribute("uri", "-s " + uri);

        String ipAddr = request.getRemoteAddr();
        String hostname = request.getRemoteHost();
        String user = KeycloakUtil.getUserName(principal, userClaim);
        Issue issue = new Issue(user, ipAddr, hostname);
        issueRepository.save(issue);
        model.addAttribute("date", formatter.format(issue.getDate()));
        model.addAttribute("offset", OffsetDateTime.now().getOffset());

        model.addAttribute("redundancy", false);

        if (replicatedServers != null && !"".equals(replicatedServers)) {
        	String[] servers = replicatedServers.split(" ");
        	String otherUrl = "-s " + uri;
        	boolean otherExist = false;

        	for (String server: servers) {
        		if (!uri.equals(server)) {
        			otherExist = true;
            		otherUrl += (" -s " + server);
        		}
        	}

            model.addAttribute("redundancy", otherExist);
            model.addAttribute("otherUrl", otherUrl);
        }
        return "passphrase";
    }

    @GetMapping(path = "/errors")
    public String getErrros(Principal principal, Model model, Pageable pageable) {
        String user = KeycloakUtil.getUserName(principal, userClaim);
        Page<Error> errors = errorRepository.findByUserOrderByIdDesc(pageable, user);
        model.addAttribute("page", errors);
        model.addAttribute("errors", errors.getContent());
        model.addAttribute("url", "errors");
        model.addAttribute("offset", OffsetDateTime.now().getOffset());

        return "errors";
    }

    @GetMapping(path = "/issues")
    public String getIssues(Principal principal, Model model, Pageable pageable) {
        String user = KeycloakUtil.getUserName(principal, userClaim);
        Page<Issue> issues = issueRepository.findByUserOrderByIdDesc(pageable, user);
        model.addAttribute("page", issues);
        model.addAttribute("issues", issues.getContent());
        model.addAttribute("url", "errors");
        model.addAttribute("offset", OffsetDateTime.now().getOffset());

        return "issues";
    }

    @GetMapping(path = "/logout")
    public String logout(HttpServletRequest request) throws ServletException {
        request.logout();
        return "redirect:menu";
    }
}
