package com.likeyichu.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;

@Component
public class Dao implements BeanFactoryAware{
	final static Logger logger=Logger.getLogger(Dao.class);
	private static DataSource  dataSource;
	private static Connection connection;
	private static Set<String> urlSet=new HashSet<String>();

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		dataSource=beanFactory.getBean("dataSource", DataSource.class);
		try {
			connection=dataSource.getConnection();
		} catch (SQLException e) {
			logger.error("得到connection失败"+e.toString());
		}
		logger.info("setBeanFactory被触发，得到bean：dataSource");
		try {
			initSet();
			logger.info("初始化urlSet成功");
		} catch (SQLException e) {
			logger.error("初始化urlSet失败"+e.toString());
		}
	}
	void initSet() throws SQLException{
		Statement sm=connection.createStatement();
		ResultSet rs=sm.executeQuery("select URL from `collect_positive_table`");
		while(rs.next()){
			urlSet.add(rs.getString("URL"));
		}
		rs.close();
		rs=sm.executeQuery("select URL from `collect_negative_table`");
		while(rs.next()){
			urlSet.add(rs.getString("URL"));
		}
		rs.close();
	}
	int maxNo() throws SQLException{
		Statement sm=connection.createStatement();
		ResultSet rs=sm.executeQuery("select max(no) from `collect_positive_table`");
		rs.next();//这行不能少
		int result=rs.getInt(1);
		rs.close();
		sm.close();
		return result;
	}
	int maxId() throws SQLException{
		Statement sm=connection.createStatement();
		ResultSet rs=sm.executeQuery("select max(id) from `collect_positive_table`");
		rs.next();
		int result1=rs.getInt(1);
		rs=sm.executeQuery("select max(id) from `collect_negative_table`");
		rs.next();
		int result2=rs.getInt(1);
		return Math.max(result1,result2);
	}
	public int insert(String url,String title,String content,boolean isPositive){
		int id=-1; 
		//若有重复url，不予插入
		if(urlSet.contains(url))
			return id;
		 String sql="insert into `collect_positive_table` (no,id,isURL,URL,isLocal,content,time) values (?,?,?,?,?,?,?) ";
		 PreparedStatement ps;
		
		 DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
		 String sqlDate=format1.format(new Date());
		try {
			ps = connection.prepareStatement(sql);
			id=maxId()+1;
			ps.setInt(1, maxNo()+1);
			ps.setInt(2,id);
			ps.setBoolean(3, true);
			ps.setString(4, url);
			ps.setBoolean(5, false);
			ps.setString(6, content);
			ps.setString(7, sqlDate);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			logger.error("插入失败"+e.toString());
			return -1;
		}
		return id;
	}
}
