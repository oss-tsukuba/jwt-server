package org.oss_tsukuba.dao;

import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "issues")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Issue {

    public static final int PASSPHRASE = 0;

    public static final int CHANGE_PASSPHRASE = 1;

    public static final int TOKEN = 2;


    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "user")
    private String user;

    @Column(name = "ip_addr")
    private String ipAddr;

    @Column(name = "hostname")
    private String hostname;

    @Column(name = "date")
    private Date date;

    @Column(name = "type")
    private int type;

    static private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Issue(String user, String ipAddr, String hostname, int type) {
        super();
        this.user = user;
        this.ipAddr = ipAddr;
        this.hostname = hostname;
        this.date = new Date();
        this.type = type;
    }

    public String getDispDate() {
        return df.format(date);
    }

    public String getDispType() {
        switch (type) {
        case PASSPHRASE:
            return "passphrase";
        case CHANGE_PASSPHRASE:
            return "change paaphrase";
        case TOKEN:
            return "access token";
        }

        return "others";
    }
}
