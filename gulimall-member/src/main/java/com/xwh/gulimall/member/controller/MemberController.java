package com.xwh.gulimall.member.controller;

import com.xwh.common.exception.BizCodeEnum;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.R;
import com.xwh.gulimall.member.entity.MemberEntity;
import com.xwh.gulimall.member.exception.PhoneExsitException;
import com.xwh.gulimall.member.exception.UsernameExistException;
import com.xwh.gulimall.member.service.MemberService;
import com.xwh.gulimall.member.vo.MemberLoginVo;
import com.xwh.gulimall.member.vo.MemberRegistVo;
import com.xwh.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


/**
 * 会员
 *
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 13:28:19
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo) {
        MemberEntity entity = memberService.login(vo);
        if (entity != null) {
            return R.ok();
        } else {
            return R.error(BizCodeEnum.LOGINACCT_PASSWORD_INVAILD_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PASSWORD_INVAILD_EXCEPTION.getMessage());
        }
    }

    @PostMapping("/oauth2/login")
    public R oauth2Login(@RequestBody SocialUser socialUser) {
        MemberEntity entity = null;
        try {
            entity = memberService.login(socialUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (entity != null) {
            return R.ok().setDate(entity);
        } else {
            return R.error(BizCodeEnum.LOGINACCT_PASSWORD_INVAILD_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PASSWORD_INVAILD_EXCEPTION.getMessage());
        }
    }

    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegistVo vo) {
        try {
            memberService.regist(vo);
        } catch (PhoneExsitException e) {
            return R.error(BizCodeEnum.PHONE_CODE_EXCEPTION.getCode(), BizCodeEnum.PHONE_CODE_EXCEPTION.getMessage());
        } catch (UsernameExistException e) {
            return R.error(BizCodeEnum.USER_EXIST_EXCEPTION.getCode(), BizCodeEnum.USER_EXIST_EXCEPTION.getMessage());
        }
        return R.ok();
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member) {
        memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member) {
        memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
