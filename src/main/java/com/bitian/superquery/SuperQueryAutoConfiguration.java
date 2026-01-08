package com.bitian.superquery;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author admin
 */
@Configuration
@EnableConfigurationProperties({MyProperties.class})
@ConditionalOnBean(SqlSessionFactory.class)
@AutoConfigureAfter(name={"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration.class","com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration"})
public class SuperQueryAutoConfiguration implements InitializingBean {

    @Resource
    MyProperties myProperties;

    private final List<SqlSessionFactory> sqlSessionFactoryList;

    public SuperQueryAutoConfiguration(List<SqlSessionFactory> sqlSessionFactoryList) {
        this.sqlSessionFactoryList = sqlSessionFactoryList;
    }

    public void afterPropertiesSet() throws Exception {
        if(myProperties.getEnable()==false)
            return;
        SuperQueryInterceptor interceptor=new SuperQueryInterceptor(myProperties);
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
            List<Interceptor> exists=findExists(configuration,interceptor);
            if(!exists.isEmpty())
                configuration.getInterceptors().removeAll(exists);
            configuration.addInterceptor(interceptor);
        }
    }

    private List<Interceptor> findExists(org.apache.ibatis.session.Configuration configuration, Interceptor interceptor) {
        return configuration.getInterceptors().stream().filter(config->interceptor.getClass().isAssignableFrom(config.getClass())).collect(Collectors.toList());
    }
}
