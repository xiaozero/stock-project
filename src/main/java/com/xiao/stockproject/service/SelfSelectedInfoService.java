package com.xiao.stockproject.service;

import com.xiao.stockproject.entity.SelfSelectedInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 */
public interface SelfSelectedInfoService extends IService<SelfSelectedInfo> {

     void updateStackInfoReal();
}
