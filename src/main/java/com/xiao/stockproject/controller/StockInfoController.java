package com.xiao.stockproject.controller;

import com.xiao.stockproject.entity.vo.Result;
import com.xiao.stockproject.service.SelfSelectedInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.io.IOException;
import java.util.Map;

/**
 * @author xiaohuijia
 * @date 2026/1/12 8:51
 */
@Api(value = "股票信息管理", tags = "股票信息管理")
@RestController
@RequestMapping("/api/stock")
public class StockInfoController {
    @Autowired
    private SelfSelectedInfoService selfSelectedInfoService;
    /**
     *  获取股票价格
     * @return
     * @throws Exception
     */
    @ApiOperation(value="获取股票价格", notes="获取股票价格")
    @PostMapping("/getStockRealPrice")
    public Result<Object> getStockRealPrice() throws Exception {
        selfSelectedInfoService.updateStackInfoReal();
        return Result.ok("查询成功");
    }
}


