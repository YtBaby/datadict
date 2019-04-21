package com.jf.datadict.util;
 
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
 
import com.alibaba.druid.pool.DruidDataSource;
import com.jf.datadict.constants.StaticConstants;
import com.jf.datadict.exception.ServiceException;
import com.jf.datadict.model.MySqlVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 通用数据库工具类,基于Druid连接池实现
 * 包含以下功能
 * 1.获取连接
 * 2.关闭资源
 * 3.执行通用的更新操作
 * 4.执行通用的查询列表操作
 * 5.执行通用的查询单条记录操作
 * @author Luo
 * https://blog.csdn.net/LxxImagine/article/details/81604408
 */

@PropertySource("classpath:application-dev.properties")
public class DBUtils {

	private static HttpSession session;
	//声明druid连接池对象
	private static DruidDataSource pool;

	/**数据库 链接URL地址**/
	private static String url;
	/**账号**/
	private static String username;
	/**密码**/
	private static String password;
	/**初始连接数**/
	@Value("${spring.datasource.initialSize}")
	private static int initialSize = 5;
	/**最大活动连接数**/
	@Value("${spring.datasource.maxActive}")
	private static int maxActive = 50;
	/**最小闲置连接数**/
	@Value("${spring.datasource.minIdle}")
	private static int minIdle = 5;
	/**连接耗尽时最大等待获取连接时间**/
	@Value("${spring.datasource.maxWait}")
	private static long maxWait = 60000;
	/**配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒*/
	private static long timeBetweenEvictionRunsMillis = 60000;
	/**配置一个连接在池中最小生存的时间，单位是毫秒*/
	private static long minEvictableIdleTimeMillis = 10000;
	
	static {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		session = request.getSession();
		init();
	}
	
	/**
	 * 获取内存中的表单提交过来的配置内容，将其设置给连接信息
	 */
	private static void loadProp() {
        url = StaticConstants.DB_MYSQL_MAP.get("url");
        username = StaticConstants.DB_MYSQL_MAP.get("username");
        password = StaticConstants.DB_MYSQL_MAP.get("password");
	}
	
	private static void init() {
		pool = new DruidDataSource();
		//初始化配置
		loadProp();
		pool.setUrl(url);
		pool.setUsername(username);
		pool.setPassword(password);
		
		//设置连接池中初始连接数
		pool.setInitialSize(initialSize);
		//设置最大连接数
		pool.setMaxActive(maxActive);
		//设置最小的闲置链接数
		pool.setMinIdle(minIdle);
		//设置最大的等待时间(等待获取链接的时间)
		pool.setMaxWait(maxWait);
		pool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
		pool.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		// 标记是否删除泄露的连接,如果他们超过了removeAbandonedTimout的限制
		pool.setRemoveAbandoned(true);
		// 设置120秒为超时时间，超时强制回收。感觉小场景可以这么用，但实际并不建议。
		pool.setRemoveAbandonedTimeout(120);
	}
	
	/**
	 * 链接获取
	 * @return
	 */
	public static Connection getConn() {
		try {
			if (session.getAttribute("back_url") == null || !session.getAttribute("back_url").equals(url)) {
				init();
				// System.out.println("进来了.................");
			}
			// 此处使用不好的解决方式。可能会比较耗费性能吧，不是很懂毕竟能力有限，不知道要如何设置。。。暂且就写成再次强制初始化吧
			System.out.println("连接数："+pool.getActiveCount());
			if (pool.getActiveCount() == maxActive-1) {
				pool.close();
				System.out.println("关闭了");
			}
			//如果连接池为空或者被异常关闭,则重新初始化一个
			if(pool == null || pool.isClosed()) {
				init();
				System.out.println("初始化");
			}
			return pool.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
 
	/**
	 * 资源关闭
	 * 
	 * @param stmt
	 * @param conn
	 */
	public static void close(Statement stmt, Connection conn) {
		try {
			if (stmt != null) {
				stmt.close();
			}
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
 
	/**
	 * 根据给定的带参数占位符的SQL语句，创建 PreparedStatement 对象
	 *
	 * @param SQL
	 *            带参数占位符的SQL语句
	 * @return 返回相应的 PreparedStatement 对象
	 */
	private static PreparedStatement prepare(String SQL, boolean autoGeneratedKeys) {
		Connection conn = getConn();
		if (conn == null) {
			return null;
		}
		PreparedStatement ps = null;
		/* 设置事务的提交方式 */
		try {
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("设置事务的提交方式为:手动提交时失败: " + e.getMessage());
		}
		try {
			if (autoGeneratedKeys) {
				ps = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);
			} else {
				ps = conn.prepareStatement(SQL);
			}
		} catch (SQLException e) {
			System.out.println("创建 PreparedStatement 对象失败: " + e.getMessage());
		}

		return ps;

	}

	public static Statement statement() {
		Connection conn = getConn();
		if (conn == null) {
			return null;
		}
		Statement st = null;
		/* 设置事务的提交方式 */
		try {
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("设置事务的提交方式为:手动提交时失败: " + e.getMessage());
		}
		try {
			st = conn.createStatement();
		} catch (SQLException e) {
			System.out.println("创建 Statement 对象失败: " + e.getMessage());
		}

		return st;
	}

	public static ResultSet query(String SQL, Object... params) {
		if (SQL == null || SQL.trim().isEmpty() || !SQL.trim().toLowerCase().startsWith("select")) {
			throw new RuntimeException("你的SQL语句为空或不是查询语句");
		}
		ResultSet rs = null;
		if (params.length > 0) {
			/* 说明 有参数 传入，就需要处理参数 */
			PreparedStatement ps = prepare(SQL, false);
			try {
				for (int i = 0; i < params.length; i++) {
					ps.setObject(i + 1, params[i]);
				}
				rs = ps.executeQuery();
			} catch (SQLException e) {
				System.out.println("执行SQL失败: " + e.getMessage());
			}
		} else {
			/* 说明没有传入任何参数 */
			Statement st = statement();
			try {
				rs = st.executeQuery(SQL); // 直接执行不带参数的 SQL 语句
			} catch (SQLException e) {
				System.out.println("执行SQL失败: " + e.getMessage());
			}
		}

		return rs;
	}

	public static Boolean validauteMySqlConnection(MySqlVO vo){
		String driverClassName = "com.mysql.jdbc.Driver";
		try {
			Class.forName(driverClassName);
			String url = "jdbc:mysql://" + vo.getIp() + ":" + vo.getPort() + "/mysql?useSSL=false";
			Connection conn= DriverManager.getConnection(url,vo.getUserName(),vo.getPwd());
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}
