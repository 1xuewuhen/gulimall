package com.xwh.gulimall.member.dao;

import com.xwh.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 13:28:19
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
