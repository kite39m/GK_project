package com.zwy.gk_backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zwy.gk_backend.entity.ShenlunEssay;

public interface ShenlunService {

    ShenlunEssay submitEssay(Integer userId, String topic, String content);

    IPage<ShenlunEssay> getEssayHistory(Integer userId, int page, int size);
}
