package com.jay.englishpracticeplatform.security;

import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

//登陆成功后，代表[当前用户] 的对象， 会被保存在 Session 中
@Getter
public class AuthUser implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String username;
    private String password;        //存的是密码哈希，登陆完成后会被清除

    public AuthUser(Long id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.password = passwordHash;
    }

    //用户拥有的权限。目前所有用户都是普通用户
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    //登陆验证完成后，Spring Security 会调用这个方法，清楚密码
    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
