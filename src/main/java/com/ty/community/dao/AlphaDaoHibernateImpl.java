package com.ty.community.dao;

import org.springframework.stereotype.Repository;

@Repository("alphaHibernate")    //能够被spring扫描到的处理数据库相关的注解,给这个bean自定义名字能够让spring强制返回这个bean内容
public class AlphaDaoHibernateImpl implements AlphaDao{
    @Override
    public String select() {
        return "Hibernate";
    }
}
