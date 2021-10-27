package org.oss_tsukuba.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "passphrases")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Passphrase {

	@Id
	@Column(name = "user")
	private String user;

	@Column(name = "phrase")
	private String phrase;
	
	@Column(name = "iv")
	private String iv;
}
