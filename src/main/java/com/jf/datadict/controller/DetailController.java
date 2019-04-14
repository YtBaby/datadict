package com.jf.datadict.controller;

import com.jf.datadict.entity.DataBaseName;
import com.jf.datadict.model.JSONResult;
import com.jf.datadict.service.DetailService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@Controller
public class DetailController {

    @Resource
    private DetailService detailService;

    @GetMapping("/show")
    public String show(HttpSession httpSession, @RequestParam("dbName") String dbName){
        httpSession.setAttribute("dbName", dbName);
        return "show";
    }

    @ResponseBody
    @PostMapping("/refreshToShow")
    public JSONResult refreshToShow(HttpSession httpSession, @RequestParam("db_id") String dbId){
        DataBaseName res = detailService.queryOneDBName(dbId);
        httpSession.setAttribute("dbName", res.getName());
        return JSONResult.ok(res);
    }

    @ResponseBody
    @PostMapping("/queryMenuList")
    public JSONResult queryMenuList(String dbName){
        return detailService.queryMenuList(dbName);
    }

    @ResponseBody
    @PostMapping("/queryTableStructure")
    public JSONResult queryTableStructure(@RequestParam("db_name") String dataBaseName,
                                          @RequestParam("table_name") String tableName){
        return detailService.queryTableStructure(dataBaseName, tableName);
    }
}
