package com.ty.community.service;

import com.ty.community.dao.LoginTicketMapper;
import com.ty.community.dao.UserMapper;
import com.ty.community.entity.LoginTicket;
import com.ty.community.entity.User;
import com.ty.community.util.CommunityConstant;
import com.ty.community.util.CommunityUtil;
import com.ty.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 2.4.4.Service层
 * UserService类中添加login，logout方法，分别提供登录和登出服务，代码见下。
 * 2.5.3.service层
 * UserService中添加findLoginTicket方法。
 * 2.6设置用户的头像和修改密码功能
 * 2.6.1.service层
 * UserService中添加updateHeader方法和changePassword方法。
 */
@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;
    /*@Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;*/

    @Autowired
    private LoginTicketMapper loginTicketMapper;


    public User findUserById(int id){
        return userMapper.selectById(id);
    }

    /**
     * 实现提交注册的请求，在服务端对提交的数据进行验证，通过后发送激活邮件。
     * @param user
     * @return
     */
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }

        // 验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }

        // 验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.ty.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    /**
     * 实现激活账号的请求，在服务端验证激活码的有效性，若有效则修改账号的状态。
     * 1.已经存在该用户了，则返回重复激活
     * 2.如果激活码是正确得激活码，则返回激活成功
     * 3.不是上面两种情况，则返回false
     * @param userId
     * @param code
     * @return
     */
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    /**
    *2.4.4.Service层
     *  * UserService类中添加login方法，分别提供登录服务，代码见下。
     */
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }

        /**
         * 2.5.1 5.1登录完成的操作
         * 1.用户在页面中提交登录信息表单，访问”/login“，发送post请求
         *
         * 【首先读取session中存储的验证码信息，对验证码进行合法性判断，如果验证码非法则返回错误提示信息】，返回前端login页面。
         *
         * 2.获取用户登录信息过期时间（登录表单中记住我按钮）
         *
         * 3.调用Service层的login服务（将步骤1中的【】部分下沉至步骤3也是可以的，这时步骤1中的“返回前端login页面”可放入最后判断）
         *
         *      3.1第一阶段合法判断：先对前端传入的username，password进行判空操作，如果非法则将相应提示信息存入map中返回至Controller层。
         *
         *      3.2第二阶段合法判断：通过第一轮合法判断，调用dao层的selectByName方法获取用户，
         *
         *       如果查不到用户则返回“该账号不存在！”错误提示信息，如果用户激活状态为0，则返回“该账号未激活！”错误提示信息。
         *
         *       3.3第三阶段合法判断：通过第二轮合法判断，开始校验用户的密码，代码段见下：

         */
        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        //(2.5.1)3.4正式的登录操作：生成登录凭证，并插入到数据库中，代码段见下：
        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        loginTicketMapper.insertLoginTicket(loginTicket);

        //(2.5.1)3.5将登录凭证加入到map中，返回map。
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    /**
     *2.4.4.Service层
     *  * UserService类中添加logout方法，分别提供登出服务，代码见下。
     */
    public void logout(String ticket) {
        loginTicketMapper.updateStatus(ticket, 1);
    }
    /**
     *2.5.3.service层
     * UserService中添加findLoginTicket方法。
     */
    public LoginTicket findLoginTicket(String ticket) {
        return loginTicketMapper.selectByTicket(ticket);
    }

   /**
    *  2.6设置用户的头像和修改密码功能
    *  2.6.1.service层
    *  UserService中1.>添加updateHeader方法和2.>changePassword方法。
    */


    //UserService中添加2.>changePassword方法。
    //用户修改密码服务

    /**
     *
     * @return
     *    public Map<String,Object> changePassword(){
     *
     *
     *     }
     */




}
