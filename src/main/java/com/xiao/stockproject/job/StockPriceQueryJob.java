package com.xiao.stockproject.job;

import com.xiao.stockproject.entity.SelfSelectedInfo;
import com.xiao.stockproject.service.SelfSelectedInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.awt.*;

/**
 * @author xiaohuijia
 * @date 2026/2/24 15:22
 */
@Slf4j
@Component
public class StockPriceQueryJob  {
    @Autowired
    private SelfSelectedInfoService selfSelectedInfoService;
    @Scheduled(cron = "0 */1 9-15 * * ?")
    public void queryPrice(){
        log.info("开始执行查询价格定时任务");
        selfSelectedInfoService.updateStackInfoReal();
        log.info("查询价格定时任务结束");
    }
}
