package com.zwy.gk_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwy.gk_backend.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
