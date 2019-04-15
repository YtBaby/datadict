package com.jf.datadict.controller;

import com.jf.datadict.constants.ReturnCode;
import com.jf.datadict.model.JSONResult;
import com.jf.datadict.service.DetailService;
import com.jf.datadict.service.DataStatisticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
public class IndexController {

    @Resource
    private DetailService dataBaseService;

    @Resource
    private DataStatisticsService dataStatisticsService;

    @GetMapping("/")
    public String index(Model model){
        JSONResult res = dataStatisticsService.queryDataStatistics();
        if (res.getStatus().equals(ReturnCode.Code_200)) {
            model.addAttribute("dataStatistics", res.getData());
        }
        return "index";
    }

    @ResponseBody
    @GetMapping("/queryAllDataBase")
    public JSONResult queryAllDataBase() {
        return dataBaseService.queryAllDataBase();
    }

    @ResponseBody
    @GetMapping("/queryDataStatistics")
    public JSONResult queryDataStatistics() {
        return dataStatisticsService.queryDataStatistics();
    }
}
