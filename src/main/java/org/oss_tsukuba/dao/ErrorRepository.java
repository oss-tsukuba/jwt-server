package org.oss_tsukuba.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorRepository extends JpaRepository<Error, Integer>, JpaSpecificationExecutor<Error> {

	public List<Error> findByUserOrderByIdDesc(String user);

}
