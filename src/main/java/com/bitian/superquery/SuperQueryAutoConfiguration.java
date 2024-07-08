package com.bitian.superquery;

import com.bitian.common.util.ThreadUtil;
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
@AutoConfigureAfter(org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration.class)
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
        ThreadUtil.executeThread(()->{
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            SuperQueryInterceptor interceptor=new SuperQueryInterceptor(myProperties);
            for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
                org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
                List<Interceptor> exists=findExists(configuration,interceptor);
                if(exists.size()>0)
                    configuration.getInterceptors().removeAll(exists);
                configuration.addInterceptor(interceptor);
            }
        });
    }

    private List<Interceptor> findExists(org.apache.ibatis.session.Configuration configuration, Interceptor interceptor) {
        return configuration.getInterceptors().stream().filter(config->interceptor.getClass().isAssignableFrom(config.getClass())).collect(Collectors.toList());
    }
}
