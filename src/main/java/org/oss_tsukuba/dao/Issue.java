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

    static private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Issue(String user, String ipAddr, String hostname) {
        super();
        this.user = user;
        this.ipAddr = ipAddr;
        this.hostname = hostname;
        this.date = new Date();
    }

    public String getDispDate() {
        return df.format(date);
    }

}
