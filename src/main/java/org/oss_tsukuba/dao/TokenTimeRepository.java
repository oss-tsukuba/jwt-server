package org.oss_tsukuba.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenTimeRepository extends JpaRepository<TokenTime, String> {
}
