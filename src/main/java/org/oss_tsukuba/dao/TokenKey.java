package org.oss_tsukuba.dao;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenKey implements Serializable {
	
	/**
	 * Serial
	 */
	private static final long serialVersionUID = 3771474388079967512L;

	private String user;
	
	private String audience;
	
}
