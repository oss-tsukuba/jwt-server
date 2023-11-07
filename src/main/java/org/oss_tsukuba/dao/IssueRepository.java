package org.oss_tsukuba.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Integer>, JpaSpecificationExecutor<Error> {

    public Page<Issue> findByUserOrderByIdDesc(Pageable pageable, String user);

}
