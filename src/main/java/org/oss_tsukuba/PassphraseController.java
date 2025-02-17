package org.oss_tsukuba;

import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.oss_tsukuba.dao.Error;
import org.oss_tsukuba.dao.ErrorRepository;
import org.oss_tsukuba.dao.Issue;
import org.oss_tsukuba.dao.IssueRepository;
import org.oss_tsukuba.dao.TokenTime;
import org.oss_tsukuba.dao.TokenTimeRepository;
import org.oss_tsukuba.service.TokenService;
import org.oss_tsukuba.utils.IssueUtil;
import org.oss_tsukuba.utils.KeycloakUtil;
import org.oss_tsukuba.utils.PropUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PassphraseController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ErrorRepository errorRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private TokenTimeRepository tokenTimeRepository;

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
    public String getMenu(Principal principal, Model model) {
        String user = KeycloakUtil.getUserName(principal, userClaim);

        model.addAttribute("version", version);

        Optional<TokenTime> opt = tokenTimeRepository.findById(user);
        TokenTime tokenTime = null;

        if (opt.isPresent()) {
            tokenTime = opt.get();
        }

        if (tokenTime == null ||
                tokenTime.getLogoutAt() != 0) {
            model.addAttribute("dipGen", true);
        } else {
            model.addAttribute("dipGen", false);
            Instant instant = Instant.ofEpochSecond(tokenTime.getLoginAt());
            Date date = Date.from(instant);
            model.addAttribute("date", formatter.format(date));
        }

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

        if (hostname.equals(ipAddr)) {
            hostname = "";
        }

        String user = KeycloakUtil.getUserName(principal, userClaim);
        Issue issue = new Issue(user, ipAddr, hostname, Issue.PASSPHRASE);
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

        IssueUtil.udpateIssue(issueRepository, issue);

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
        Page<Issue> issues = issueRepository.findByUserOrderByDateDesc(pageable, user);
        model.addAttribute("page", issues);
        model.addAttribute("issues", issues.getContent());
        model.addAttribute("url", "issues");
        model.addAttribute("offset", OffsetDateTime.now().getOffset());

        return "issues";
    }

}
