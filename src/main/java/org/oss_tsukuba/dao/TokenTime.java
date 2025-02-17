package org.oss_tsukuba.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "token_time")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TokenTime {

    @Id
    @Column(name = "user")
    private String user;

    @Column(name = "login_at")
    private long loginAt;

    @Column(name = "logout_at")
    private long logoutAt;
}
