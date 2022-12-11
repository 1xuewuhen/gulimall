package com.xwh.gulimall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;
import com.xwh.gulimall.member.dao.MemberDao;
import com.xwh.gulimall.member.dao.MemberLevelDao;
import com.xwh.gulimall.member.entity.MemberEntity;
import com.xwh.gulimall.member.entity.MemberLevelEntity;
import com.xwh.gulimall.member.exception.PhoneExsitException;
import com.xwh.gulimall.member.exception.UsernameExistException;
import com.xwh.gulimall.member.service.MemberService;
import com.xwh.gulimall.member.vo.MemberLoginVo;
import com.xwh.gulimall.member.vo.MemberRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {


    @Autowired
    private MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {
        MemberEntity memberEntity = new MemberEntity();
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(levelEntity.getId());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        memberEntity.setPassword(encoder.encode(vo.getPassword()));
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());
        memberEntity.setUsername(vo.getUserName());
        memberEntity.setMobile(vo.getPhone());
        baseMapper.insert(memberEntity);
    }

    @Override
    public void checkUserNameUnique(String userName) throws UsernameExistException {
        Integer integer = this.baseMapper.selectCount(new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getUsername, userName));
        if (integer > 0) {
            throw new UsernameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        MemberEntity entity = this.baseMapper.selectOne(new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getUsername,
                vo.getLoginAcct()).or().eq(MemberEntity::getMobile, vo.getLoginAcct()));
        if (entity!=null){
            boolean matches = encoder.matches(vo.getPassword(), entity.getPassword());
            if (matches){
                return entity;
            }else {
                return null;
            }
        }else {
            return null;
        }
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExsitException {
        Integer integer = baseMapper.selectCount(new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getMobile, phone));
        if (integer > 0) {
            throw new PhoneExsitException();
        }
    }

}