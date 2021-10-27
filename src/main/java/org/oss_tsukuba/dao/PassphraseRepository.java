package org.oss_tsukuba.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PassphraseRepository extends JpaRepository<Passphrase, String> {
}
