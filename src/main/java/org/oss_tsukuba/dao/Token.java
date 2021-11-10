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
@Table(name = "tokens")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Token {

	@Id
	@Column(name = "user")
	private String user;

	@Column(name = "token")
	private String token;
	
	@Column(name = "iv")
	private String iv;
}
