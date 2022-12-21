package com.xwh.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.utils.HttpUtils;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;
import com.xwh.gulimall.member.config.GiteeGetUser;
import com.xwh.gulimall.member.dao.MemberDao;
import com.xwh.gulimall.member.dao.MemberLevelDao;
import com.xwh.gulimall.member.entity.MemberEntity;
import com.xwh.gulimall.member.entity.MemberLevelEntity;
import com.xwh.gulimall.member.exception.PhoneExsitException;
import com.xwh.gulimall.member.exception.UsernameExistException;
import com.xwh.gulimall.member.service.MemberService;
import com.xwh.gulimall.member.vo.MemberLoginVo;
import com.xwh.gulimall.member.vo.MemberRegistVo;
import com.xwh.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {


    @Autowired
    private MemberLevelDao memberLevelDao;

    @Autowired
    private GiteeGetUser giteeGetUser;

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
        memberEntity.setNickname(vo.getUserName());
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
        if (entity != null) {
            boolean matches = encoder.matches(vo.getPassword(), entity.getPassword());
            if (matches) {
                return entity;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        String uid = socialUser.getUid();
        MemberEntity member = this.baseMapper.selectOne(new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getSocialUid, uid));
        if (member != null) {
            MemberEntity memberEntity = new MemberEntity();
            memberEntity.setId(member.getId());
            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresId(socialUser.getExpires_in());
            this.baseMapper.updateById(memberEntity);
            member.setAccessToken(socialUser.getAccess_token());
            member.setExpiresId(socialUser.getExpires_in());
            return member;
        } else {
            MemberEntity regist = new MemberEntity();
            try {
                HashMap<String, String> map = new HashMap<>();
                map.put("access_token", socialUser.getAccess_token());
                HttpResponse response = HttpUtils.doGet(giteeGetUser.getHost(), "/api/v5/user", "get", new HashMap<String, String>(), map);
                if (response.getStatusLine().getStatusCode() == 200){
                    String s = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(s);
                    regist.setNickname(jsonObject.getString("name"));
                    regist.setLevelId(1L);
                    regist.setCreateTime(new Date());
                }
            }catch (Exception e){

            }
            regist.setSocialUid(socialUser.getUid());
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresId(socialUser.getExpires_in());
            baseMapper.insert(regist);
            return regist;
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