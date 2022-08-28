package com.ty.community.util;

import com.ty.community.entity.User;
import org.springframework.stereotype.Component;
/**
 * 2.5.1.2 在util包中创建HostHolder类
 * 持有用户信息，用于代替session对象
 */
@Component
public class HostHolder {
    private ThreadLocal<User> users=new ThreadLocal<>();

    public void setUser(User user){
        users.set(user);
    }

    public User getUser(){
        return users.get();
    }

    public void clear(){
        users.remove();
    }
}
