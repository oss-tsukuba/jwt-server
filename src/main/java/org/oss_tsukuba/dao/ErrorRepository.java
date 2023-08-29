package org.oss_tsukuba.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorRepository extends JpaRepository<Error, Integer>, JpaSpecificationExecutor<Error> {

    public Page<Error> findByUserOrderByIdDesc(Pageable pageable, String user);

}
