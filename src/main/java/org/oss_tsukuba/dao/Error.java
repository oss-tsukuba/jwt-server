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
@Table(name = "errors")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Error {

    public static final int CHECK_DIGIT_ERROR = 0;

    public static final int DECRYPT_ERROR = 1;

    public static final int LENGTH_ERROR = 2;

    public static final int CHARACTER_ERROR = 3;

    public static final int EXPIRED_ERROR = 4;

    public static final int SERVER_DOWN = 5;

    public static final int UNEXPECTED_ERROR = 6;

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


    public Error(String user, String ipAddr, String hostname, int type) {
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

    public String getError() {
        switch (type) {
        case CHECK_DIGIT_ERROR:
            return "check digit";
        case DECRYPT_ERROR:
            return "invalid passphrase";
        case LENGTH_ERROR:
            return "character count";
        case CHARACTER_ERROR:
            return "character";
        case EXPIRED_ERROR:
            return "expired";
        case SERVER_DOWN:
            return "server down";
        case UNEXPECTED_ERROR:
            return "unexpected error";
        }

        return "others";
    }
}
