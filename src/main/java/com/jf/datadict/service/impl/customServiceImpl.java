package com.jf.datadict.service.impl;

import com.jf.datadict.constants.StaticMySqlQuery;
import com.jf.datadict.entity.DictMenu;
import com.jf.datadict.exception.ServiceException;
import com.jf.datadict.model.DataBaseVO;
import com.jf.datadict.model.JSONResult;
import com.jf.datadict.service.CustomService;
import com.jf.datadict.util.DBUtil;
import com.jf.datadict.util.MongoDBUtil;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Service
public class customServiceImpl implements CustomService {

    @Override
    public JSONResult queryAllDBOfCustom(HttpSession session) {
        List<String> dataBaseNames = new ArrayList<>();
        try {
            ResultSet rs = DBUtil.query(session, StaticMySqlQuery.dbListQuery);
            while (rs.next()){
                dataBaseNames.add(rs.getString(1));

            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return JSONResult.ok(dataBaseNames);
    }

    @Override
    public JSONResult validateMySqlConnection(DataBaseVO vo) {
        Boolean resConnection = DBUtil.validauteMySqlConnection(vo);
        if (!resConnection) {
            return JSONResult.error500("Mysql数据库尝试连接失败！");
        }
        return JSONResult.ok(true);
    }

    @Override
    public JSONResult validateMongoDBConnection(DataBaseVO vo) {
        Boolean resConnection = MongoDBUtil.validateMongoDBConnect(vo);
        if (!resConnection) {
            return JSONResult.error500("Mongodb数据库尝试连接失败！");
        }
        return JSONResult.ok(true);
    }

    @Override
    public JSONResult queryMenuList(HttpSession session, String dbName) {
        List<DictMenu> resMenuList = new ArrayList<>();

        String sql = StaticMySqlQuery.getTablesQuery(dbName);
        try {
            ResultSet rs = DBUtil.query(session, sql);
            while (rs.next()){
                DictMenu menu = new DictMenu();

                int parentUid = (int) (Math.random()*(600-100));
                menu.setParentUid(parentUid);
                menu.setMenuName(rs.getString(1));
                resMenuList.add(menu);
            }
        } catch (Exception e){
            e.printStackTrace();
            throw new ServiceException("查询菜单时出错："+e.getMessage());
        }

        return JSONResult.ok(resMenuList);
    }

    @Override
    public JSONResult queryTableStructure() {
        return null;
    }
}
