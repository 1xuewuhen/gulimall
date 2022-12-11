package com.xwh.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xwh.common.utils.PageUtils;
import com.xwh.gulimall.member.entity.MemberEntity;
import com.xwh.gulimall.member.exception.PhoneExsitException;
import com.xwh.gulimall.member.exception.UsernameExistException;
import com.xwh.gulimall.member.vo.MemberLoginVo;
import com.xwh.gulimall.member.vo.MemberRegistVo;

import java.util.Map;

/**
 * 会员
 *
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 13:28:19
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExsitException;

    void checkUserNameUnique(String userName) throws UsernameExistException;

    MemberEntity login(MemberLoginVo vo);
}

