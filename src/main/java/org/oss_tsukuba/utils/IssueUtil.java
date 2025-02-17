package org.oss_tsukuba.utils;

import org.oss_tsukuba.dao.Issue;
import org.oss_tsukuba.dao.IssueRepository;

public class IssueUtil {

    public static void udpateIssue(IssueRepository repo, Issue issue) {
        Issue target = repo.findTopByUserAndIpAddrAndTypeOrderByDateDesc(issue.getUser(), issue.getIpAddr(), issue.getType());

        if (target != null) {
            issue.setId(target.getId());
        }

        repo.save(issue);
    }

}
