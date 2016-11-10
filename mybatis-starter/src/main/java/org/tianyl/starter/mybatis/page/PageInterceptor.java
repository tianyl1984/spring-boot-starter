package org.tianyl.starter.mybatis.page;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Intercepts({
		@Signature(method = "prepare", type = StatementHandler.class, args = { Connection.class, Integer.class })
})
public class PageInterceptor implements Interceptor {

	private static final Logger logger = LoggerFactory.getLogger(PageInterceptor.class);
	private static final ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();
	private static final ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
	private static final DefaultReflectorFactory Default_Reflector_Factory = new DefaultReflectorFactory();

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		StatementHandler handler = (StatementHandler) invocation.getTarget();
		MetaObject metaStatementHandler = MetaObject.forObject(handler, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY, Default_Reflector_Factory);
		// 分离代理对象链(由于目标类可能被多个拦截器拦截，从而形成多次代理，通过下面的两次循环可以分离出最原始的的目标类)
		while (metaStatementHandler.hasGetter("h")) {
			Object object = metaStatementHandler.getValue("h");
			metaStatementHandler = MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY, Default_Reflector_Factory);
		}
		// 分离最后一个代理对象的目标类
		while (metaStatementHandler.hasGetter("target")) {
			Object object = metaStatementHandler.getValue("target");
			metaStatementHandler = MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY, Default_Reflector_Factory);
		}

		MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");
		BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
		Object parameterObject = boundSql.getParameterObject();
		if (parameterObject == null) {
			return invocation.proceed();
		}
		Page page = null;
		if (parameterObject instanceof Page) {// 参数只有page对象时
			page = (Page) parameterObject;
		} else if (parameterObject instanceof MapperMethod.ParamMap) {
			@SuppressWarnings("unchecked")
			MapperMethod.ParamMap<Object> mmMap = (ParamMap<Object>) parameterObject;
			for (Object obj : mmMap.values()) {
				if (obj instanceof Page) {
					page = (Page) obj;
					break;// 不支持参数有多个page对象
				}
			}
		}
		if (page != null) {// 参数中有page的自动分页
			String sql = boundSql.getSql();
			// 重写sql
			String pageSql = buildPageSqlForMysql(sql, page).toString();
			metaStatementHandler.setValue("delegate.boundSql.sql", pageSql);

			// 采用物理分页后，就不需要mybatis的内存分页了，所以重置下面的两个参数
			metaStatementHandler.setValue("delegate.rowBounds.offset", RowBounds.NO_ROW_OFFSET);
			metaStatementHandler.setValue("delegate.rowBounds.limit", RowBounds.NO_ROW_LIMIT);
			Connection connection = (Connection) invocation.getArgs()[0];
			// 设置总数
			if (page.isAutoCount()) {
				setPageTotalCount(sql, connection, mappedStatement, boundSql, page);
			}
		}
		return invocation.proceed();
	}

	private void setPageTotalCount(String sql, Connection connection, MappedStatement mappedStatement, BoundSql boundSql, Page page) {
		// 记录总记录数
		String countSql = "select count(0) from (" + sql + ") as total";
		PreparedStatement countStmt = null;
		ResultSet rs = null;
		try {
			countStmt = connection.prepareStatement(countSql);
			BoundSql countBS = new BoundSql(mappedStatement.getConfiguration(), countSql, boundSql.getParameterMappings(), boundSql.getParameterObject());
			setParameters(countStmt, mappedStatement, countBS, boundSql.getParameterObject());
			rs = countStmt.executeQuery();
			long totalCount = 0;
			if (rs.next()) {
				totalCount = rs.getLong(1);
			}
			page.setTotalCount(totalCount);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("查询总数出错", e);
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				logger.error("结果集关闭出错", e);
			}
			try {
				countStmt.close();
			} catch (SQLException e) {
				logger.error("结果集关闭出错", e);
			}
		}

	}

	private void setParameters(PreparedStatement ps, MappedStatement mappedStatement, BoundSql boundSql, Object parameterObject) throws SQLException {
		ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
		parameterHandler.setParameters(ps);
	}

	/**
	 * mysql的分页语句
	 * 
	 * @param sql
	 * @param page
	 * @return String
	 */
	public StringBuilder buildPageSqlForMysql(String sql, Page page) {
		StringBuilder pageSql = new StringBuilder(100);
		int beginrow = (page.getPageNo() - 1) * page.getPageSize();
		pageSql.append(sql);
		pageSql.append(" limit " + beginrow + "," + page.getPageSize());
		return pageSql;
	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {

	}

}
