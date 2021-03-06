package com.jf.datadict.service.impl;

import com.jf.datadict.constants.StaticConstants;
import com.jf.datadict.constants.StaticMySqlQuery;
import com.jf.datadict.dao.DetailMapper;
import com.jf.datadict.entity.DictMenu;
import com.jf.datadict.entity.DictTableStructure;
import com.jf.datadict.exception.ServiceException;
import com.jf.datadict.model.JSONResult;
import com.jf.datadict.service.DetailService;
import com.jf.datadict.util.DBUtil;
import com.jf.datadict.util.MyStringUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DetailServiceImpl implements DetailService {

    @Resource
    private DetailMapper detailMapper;

    @Override
    public JSONResult queryAllDataBase() {
        List<String> dataBaseNames;
        try {
            dataBaseNames = detailMapper.queryAllDataBase();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("查询数据库列表出错："+e.getMessage());
        }

        List<String> res = new ArrayList<>();
        for (String s : dataBaseNames) {
            if (StaticConstants.MYSQL_DEFAULT_DBNAME.contains(s)) {
                continue;
            }
            res.add(s);
        }
        return JSONResult.ok(res);
    }

    @Override
    public JSONResult queryMenuList(HttpSession session, String dbName) {
        if (MyStringUtil.isEmpty(dbName)) {
            return JSONResult.error500("传入参数为空");
        }

        List<DictMenu> resMenuList = new ArrayList<>();
        if (session.getAttribute("url") != null) {
            String sql = StaticMySqlQuery.getTablesQuery(dbName);
            try {
                ResultSet rs = DBUtil.query(session, sql);
                while (rs.next()){
                    DictMenu menu = new DictMenu();

                    int parentUid = (int) (Math.random()*(1000-100));
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

        // 自定义配置时候的菜单显示
        resMenuList = getDefineMenu(dbName);
        return JSONResult.ok(resMenuList);
    }

    @Override
    public JSONResult queryTableStructure(HttpSession session, String dataBaseName,String tableName) {
        if (MyStringUtil.isEmpty(dataBaseName)) {
            throw new ServiceException("数据库名不可为空");
        }

        String realTableName = null;
        String tableChName = null;
        if (MyStringUtil.isNotEmpty(tableName)) {
            if (tableName.indexOf("[")>0) {
                realTableName = tableName.substring(tableName.indexOf("[") + 1, tableName.indexOf("]"));
                tableChName = tableName.substring(0, tableName.indexOf("[") + 1);
            } else {
                realTableName = tableName;
            }
        }

        List<DictTableStructure> dictTableStructures = new ArrayList<>();
        if (session.getAttribute("url") != null) {
            // 查询字段个数
            Map<String, Integer> ccMap = new HashMap<>();
            String sqlForCount = StaticMySqlQuery.countField(dataBaseName);
            try{
                ResultSet rs = DBUtil.query(session, sqlForCount);
                while (rs.next()) {
                    String t = rs.getString("t");
                    int c = rs.getInt("c");
                    ccMap.put(t, c);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceException("查询表字段个数出错："+e.getMessage());
            }

            if (ccMap.isEmpty()) {
                return JSONResult.build(100, "该库无数据表");
            }

            // 查询表名注释
            Map<String, String> commentMap = new HashMap<>();
            String sqlForComment = StaticMySqlQuery.getTableComment(dataBaseName);
            try{
                ResultSet rs = DBUtil.query(session, sqlForComment);
                while (rs.next()) {
                    String t = rs.getString("tn");
                    String c = rs.getString("tc");
                    commentMap.put(t, c);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceException("查询表名注释出错："+e.getMessage());
            }

            // 查询表字段
            String sql = StaticMySqlQuery.getTableFieldDetail(dataBaseName, realTableName);
            try {
                ResultSet rs = DBUtil.query(session, sql);
                while (rs.next()){
                    String table_name = rs.getString("table_name");
                    String column_name = rs.getString("column_name");
                    String column_type = rs.getString("column_type");
                    String isNull = rs.getString("is_nullable");
                    String column_key = rs.getString("column_key");
                    String column_default = rs.getString("column_default");
                    String column_comment = rs.getString("column_comment");
                    DictTableStructure ds = new DictTableStructure();
                    ds.setTableName(table_name);
                    ds.setField(column_name);
                    ds.setComment(column_comment);

                    if (tableChName == null) {
                        ds.setTableChName("");
                    }

                    if (column_type.indexOf("(")>0 && column_type.indexOf(")")>0) {
                        String type = column_type.substring(0, column_type.indexOf("("));
                        String length = column_type.substring(column_type.indexOf("(") + 1, column_type.indexOf(")"));
                        ds.setType(type);
                        ds.setLength(length);
                    }else{
                        ds.setType(column_type);
                        ds.setLength("0");
                    }

                    if (isNull.equals("YES")) {
                        ds.setIsNull("Y");
                    } else {
                        ds.setIsNull("N");
                    }

                    if (MyStringUtil.isNotEmpty(column_key)) {
                        if (column_key.equals("PRI")) {
                            ds.setKey("主键");
                        }
                        if (column_key.equals("MUL")) {
                            ds.setKey("索引");
                        }
                    }else{
                        ds.setKey("");
                    }

                    if (column_default == null) {
                        ds.setDefaultValue("");
                    }

                    if (tableChName == null) {
                        ds.setTableChName(commentMap.get(ds.getTableName()));
                    } else {
                        ds.setTableChName(tableChName);
                    }

                    if (ccMap.containsKey(ds.getTableName())) {
                        ds.setFieldCount(ccMap.get(ds.getTableName()));
                    }

                    dictTableStructures.add(ds);
                }
            } catch (Exception e){
                e.printStackTrace();
                throw new ServiceException("查询菜单时出错："+e.getMessage());
            }
            return JSONResult.ok(dictTableStructures);
        }

        /*-------------------------------------自定义配置时候可能会用到----------------------------------------------------*/
        // 查询字段个数
        List<DictTableStructure> columnCountRes;
        try {
            columnCountRes = detailMapper.queryTableColumnCount(dataBaseName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("查询表字段个数出错："+e.getMessage());
        }

        Map<String, Integer> ccMap = new HashMap<>();
        if (MyStringUtil.checkListNotEmpty(columnCountRes)) {
            ccMap = columnCountRes.stream().collect(Collectors.toMap(DictTableStructure::getTableName, DictTableStructure::getFieldCount));
        }

        if (ccMap.isEmpty()) {
            return JSONResult.build(100, "该库无数据表");
        }

        // 查询表名注释
        List<DictTableStructure> tableComments;
        try {
            tableComments = detailMapper.queryTableComment(dataBaseName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("查询表名注释出错："+e.getMessage());
        }

        Map<String, String> commentMap = new HashMap<>();
        if (MyStringUtil.checkListNotEmpty(tableComments)) {
            commentMap = tableComments.stream().collect(Collectors.toMap(DictTableStructure::getTableName, DictTableStructure::getTableChName));
        }

        try {
            dictTableStructures = detailMapper.queryTableStructure(dataBaseName, realTableName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("查询数据表结构出错："+e.getMessage());
        }

        if (MyStringUtil.checkListNotEmpty(dictTableStructures)) {
            for (DictTableStructure ds : dictTableStructures) {
                if (ds.getType().indexOf("(")>0 && ds.getType().indexOf(")")>0) {
                    String type = ds.getType().substring(0, ds.getType().indexOf("("));
                    String length = ds.getType().substring(ds.getType().indexOf("(") + 1, ds.getType().indexOf(")"));
                    ds.setType(type);
                    ds.setLength(length);
                }

                if (ds.getIsNull().equals("YES")) {
                    ds.setIsNull("Y");
                } else {
                    ds.setIsNull("N");
                }

                if (MyStringUtil.isNotEmpty(ds.getKey())) {
                    if (ds.getKey().equals("PRI")) {
                        ds.setKey("主键");
                    }
                    if (ds.getKey().equals("MUL")) {
                        ds.setKey("索引");
                    }
                }

                if (ds.getDefaultValue() == null) {
                    ds.setDefaultValue("");
                }

                if (tableChName == null) {
                    ds.setTableChName(commentMap.get(ds.getTableName()));
                } else {
                    ds.setTableChName(tableChName);
                }
                if (ccMap.containsKey(ds.getTableName())) {
                    ds.setFieldCount(ccMap.get(ds.getTableName()));
                }
            }
            return JSONResult.ok(dictTableStructures);
        }
        return JSONResult.ok(null);
    }

    /**
     * 递归判断生成的uid不唯一
     */
    private void checkUidOnly(Map<String, Integer> uidMap, String v, Integer uid){
        if (uidMap.containsValue(uid)) {
            uid = (int) (Math.random()*(10999-1000));
            checkUidOnly(uidMap, v, uid);
        }
        uidMap.put(v, uid);
    }

    /**
     * 递归查找子菜单
     *
     * @param id 当前菜单id
     * @param rootMenu 要查找的列表
     * @return list
     */
    private List<DictMenu> getChild(Integer id, List<DictMenu> rootMenu) {
        // 子菜单
        List<DictMenu> childList = new ArrayList<>();
        for (DictMenu menu : rootMenu) {
            // 遍历所有节点，将父菜单id与传过来的id比较
            if (menu.getParentUid().equals(id)) {
                childList.add(menu);
            }
        }
        // 把子菜单的子菜单再循环一遍
        for (DictMenu menu : childList) {// 没有url子菜单还有子菜单
            // 递归
            menu.setChildMenus(getChild(menu.getUid(), rootMenu));
        } // 递归退出条件
        if (childList.size() == 0) {
            return null;
        }
        return childList;
    }

    /**
     * 自定义配置时候的三级菜单显示
     * @param dbName
     * @return
     */
    private List<DictMenu> getDefineMenu(String dbName) {
        List<DictMenu> resMenuList = new ArrayList<>();
        List<DictMenu> dictMenus;
        try {
            dictMenus = detailMapper.queryMenuList(dbName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("查询虚拟菜单时出错："+e.getMessage());
        }

        if (MyStringUtil.checkListIsEmpty(dictMenus)) {
            List<DictTableStructure> dictTableStructures;
            try {
                dictTableStructures = detailMapper.queryTableColumnCount(dbName);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceException("查询该库所有表时出错："+e.getMessage());
            }

            for (DictTableStructure d : dictTableStructures) {
                DictMenu menu = new DictMenu();

                int parentUid = (int) (Math.random()*(600-100));
                menu.setParentUid(parentUid);
                menu.setMenuName(d.getTableName());
                resMenuList.add(menu);
            }
            return resMenuList;
        }

        Set<String> versionSet = new HashSet<>();
        for (DictMenu menu : dictMenus) {
            versionSet.add(menu.getVersion());
        }
        // 所有版本List
        List<String> versionList = new ArrayList<>(versionSet);

        // 最后的结果
        Map<String, Integer> uidMap = new HashMap<>();
        Map<Integer, List<DictMenu>> thirdChildMenusMap = new HashMap<>();
        // 设置版本作为一级菜单
        for (String v : versionList) {
            int uid = (int) (Math.random()*(10999-1000));
            checkUidOnly(uidMap, v, uid);

            List<DictMenu> tempMenuListOfVersion = new ArrayList<>();
            for (DictMenu menu : dictMenus) {
                if (menu.getVersion().equals(v)) {
                    tempMenuListOfVersion.add(menu);
                }
            }

            // 设置三级菜单(即表名)
            for (DictMenu d : tempMenuListOfVersion) {
                DictMenu thirdDictMenu = new DictMenu(d.getUid(), d.getVersion(), d.getTableName(), d.getParentUid());

                if (thirdChildMenusMap.containsKey(d.getParentUid())) {
                    thirdChildMenusMap.get(d.getParentUid()).add(thirdDictMenu);
                }else {
                    List<DictMenu> thirdChildMenus = new ArrayList<>();
                    thirdChildMenus.add(thirdDictMenu);
                    thirdChildMenusMap.put(d.getParentUid(), thirdChildMenus);
                }
            }

            List<DictMenu> secondChildMenus = new ArrayList<>();
            Set<Integer> secondUidSet = new HashSet<>();
            // 给二级菜单(即分类类目)赋值父ID为生成的uid
            Iterator<Map.Entry<Integer, List<DictMenu>>> iterator = thirdChildMenusMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, List<DictMenu>> entry = iterator.next();
                for (DictMenu sec : tempMenuListOfVersion) {
                    Integer secUid = sec.getParentUid();
                    if (entry.getKey().equals(secUid) && !secondUidSet.contains(secUid)) {
                        secondUidSet.add(secUid);
                        DictMenu secDictMenu = new DictMenu(secUid, sec.getVersion(), sec.getTypeName(), uid);
                        secDictMenu.setChildMenus(entry.getValue());
                        secondChildMenus.add(secDictMenu);
                    }
                }
            }

            DictMenu firstDictMenu = new DictMenu(uid, v, v, null);
            firstDictMenu.setChildMenus(secondChildMenus);
            resMenuList.add(firstDictMenu);
        }
        return resMenuList;
    }
}
