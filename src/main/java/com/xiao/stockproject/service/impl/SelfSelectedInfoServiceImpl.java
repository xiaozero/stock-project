package com.xiao.stockproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiao.stockproject.entity.SelfSelectedInfo;
import com.xiao.stockproject.entity.StockInfoHistory;
import com.xiao.stockproject.service.SelfSelectedInfoService;
import com.xiao.stockproject.mapper.SelfSelectedInfoMapper;
import com.xiao.stockproject.service.StockInfoHistoryService;
import com.xiao.stockproject.utils.DingDingMessageUtils;
import com.xiao.stockproject.utils.SimpleStockQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@Slf4j
@Service
public class SelfSelectedInfoServiceImpl extends ServiceImpl<SelfSelectedInfoMapper, SelfSelectedInfo>
    implements SelfSelectedInfoService{
    @Autowired
    private StockInfoHistoryService stockInfoHistoryService;
    @Override
    public void updateStackInfoReal() {
        //判断当前时间now是否大于11:30小于13:00
        if (isBetweenTime("11:30", "13:00")){
            log.info("当前时间在11:30-13:00之间不进行操作");
            return;
        }
        //查找所有自选的股票
        QueryWrapper<SelfSelectedInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        List<SelfSelectedInfo> selfSelectedInfoList = baseMapper.selectList(queryWrapper);
        //提取selfSelectedInfoList中的stockCode，并且用逗号隔开的字符串
        List<String> stockCodeList = selfSelectedInfoList.stream().map(SelfSelectedInfo::getStockCode).collect(Collectors.toList());
        String stockCodeStr = String.join(",", stockCodeList);
        List<StockInfoHistory> stockInfoHistorys =SimpleStockQuery.queryStock(stockCodeStr);
        //String message = "";
        for (StockInfoHistory stockInfoHistory : stockInfoHistorys){
                stockInfoHistoryService.save(stockInfoHistory);
                //更新当前价格
                selfSelectedInfoList.stream().filter(selfSelectedInfo -> selfSelectedInfo.getStockCode().equals(stockInfoHistory.getStockCode())).findFirst().ifPresent(selfSelectedInfo -> {
                    if ((Double.parseDouble(stockInfoHistory.getCurrentPrice()) >= selfSelectedInfo.getBuyPrice())){
                        DingDingMessageUtils.sendTextMsg(String.format("\n%s到达了回本价格%s，当前价格%s，请及时关注!!!",selfSelectedInfo.getStockName(),selfSelectedInfo.getBuyPrice(),stockInfoHistory.getCurrentPrice()),"13296400589");
                    }
                    if ((Double.parseDouble(stockInfoHistory.getCurrentPrice()) >= selfSelectedInfo.getTargetPrice())){
                        DingDingMessageUtils.sendTextMsg(String.format("\n%s到达了目标价格%s，当前价格%s，请及时关注!!!",selfSelectedInfo.getStockName(),selfSelectedInfo.getTargetPrice(),stockInfoHistory.getCurrentPrice()),"13296400589");
                    }
                    selfSelectedInfo.setCurrentPrice(Double.valueOf(stockInfoHistory.getCurrentPrice()));
                    baseMapper.updateById(selfSelectedInfo);
                });
                //message += String.format("\n"+stockInfoHistory.getStockName()+"：%s",stockInfoHistory.getCurrentPrice());
        }
        //DingDingMessageUtils.sendTextMsg(message,"13296400589");
    }

    /**
     * 判断当前时间是否在指定时间段内
     *
     * @param startTime 开始时间（格式："HH:mm"）
     * @param endTime   结束时间（格式："HH:mm"）
     * @return true 表示当前时间在时间段内，false 表示不在
     */
    public boolean isBetweenTime(String startTime, String endTime) {
        // 定义时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // 解析开始时间和结束时间
        LocalTime start = LocalTime.parse(startTime, formatter);
        LocalTime end = LocalTime.parse(endTime, formatter);

        // 获取当前时间
        LocalTime now = LocalTime.now();

        // 判断当前时间是否在时间段内
        if (start.isBefore(end)) {
            // 情况一：时间段不跨天（例如 09:00 - 15:00）
            return !now.isBefore(start) && !now.isAfter(end);
        } else {
            // 情况二：时间段跨天（例如 22:00 - 02:00）
            return !now.isBefore(start) || !now.isAfter(end);
        }
    }
}




