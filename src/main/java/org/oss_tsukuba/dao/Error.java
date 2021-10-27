package org.oss_tsukuba.dao;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "errors")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Error {

	@Id
	@Column(name = "id")
	private int id;

	@Column(name = "user")
	private String user;

	@Column(name = "date")
	private Date date;

	@Column(name = "type")
	private int type;

	static private SimpleDateFormat df = new SimpleDateFormat("yyyy MM/dd HH:mm:ss");

	public String getDispDate() {
		return df.format(date);
	}

	public String getError() {
		switch (type) {
		case 0:
			return "check digit エラー";
		case 1:
			return "復号化エラー";
		}

		return "その他";
	}
}
