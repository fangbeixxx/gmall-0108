package com.atguigu.gmall.ums.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }
    //验证账号是否存在 是否可用
    @Override
    public Boolean check(String data, Integer type) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        switch (type){
            case 1:queryWrapper.eq("username",data);break;
            case 2:queryWrapper.eq("phone",data);break;
            case 3:queryWrapper.eq("email",data);break;
            default:
                return null;
        }
        int count = this.count(queryWrapper);
//      表示验证通过true
        return count>0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        // 验证码
        //生成盐
        UUID uuid = UUID.randomUUID();
        String salt = StringUtils.substring(uuid.toString(), 0, 6);
        //加盐
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword()+salt));
        // 注册账号
        userEntity.setSalt(salt);
        userEntity.setLevelId(1l);
        userEntity.setNickname(userEntity.getUsername());
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(2000);
        userEntity.setStatus(1);
        userEntity.setCreateTime(new Date());
        //存入数据库
        this.save(userEntity);
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<UserEntity>()
                .eq("username", loginName).or()
                .eq("phone", loginName).or()
                .eq("email", loginName);
        List<UserEntity> userEntities = this.list(queryWrapper);

        if(CollectionUtils.isEmpty(userEntities)){
            return null;
        }
//        输入密码加盐与数据库查出的数据密码比较
        for (UserEntity userEntity : userEntities) {
            if(StringUtils.equals(DigestUtils.md5Hex(password + userEntity.getSalt()),userEntity.getPassword())){
                return userEntity;
            }
        }
        return null;
    }
}