package com.ty.community.service;

import com.ty.community.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

//@Scope("prototype")    //作用范围:prototype每次访问bean，都会创建一个实例 通常情况下都会使用单例的方式，默认就是单例
@Service   //一般业务代码用Service 注解
public class AlphaService {
    @Autowired
    private AlphaDao alphaDao;
    public AlphaService(){
        System.out.println("实例化AlphaService");
    }
    @PostConstruct  //意思是在构造器之后调用
    public void init(){
        System.out.println("初始化AlphaService");
    }

    @PreDestroy  //在销毁之前调用
    public void destory(){
        System.out.println("销毁AlphaService");
    }

    public String find(){
        return alphaDao.select();
    }
}
