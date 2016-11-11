package org.tianyl.starter.mybatis;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tianyl.starter.mybatis.page.PageInterceptor;

import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.spring.mapper.MapperScannerConfigurer;

@Configuration
public class MybatisAutoConfiguration {

	// @Bean
	public MapperScannerConfigurer mapperScannerConfigurer() {
		MapperScannerConfigurer configurer = new MapperScannerConfigurer();
		configurer.setBasePackage("com.tianyl");
		configurer.setMarkerInterface(Mapper.class);
		Properties properties = new Properties();
		properties.setProperty("mappers", Mapper.class.getName());
		properties.setProperty("style", "normal");
		configurer.setProperties(properties);
		return configurer;
	}

	@Bean
	public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
		SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
		bean.setDataSource(dataSource);
		List<Interceptor> interceptors = new ArrayList<>();
		// 分页插件
		Interceptor pageInter = new PageInterceptor();
		interceptors.add(pageInter);

		bean.setPlugins(interceptors.toArray(new Interceptor[] {}));
		// xml配置文件，无需配置文件
		// ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		// bean.setMapperLocations(resolver.getResources("classpath:mapper/*.xml"));

		return bean.getObject();
	}

}
