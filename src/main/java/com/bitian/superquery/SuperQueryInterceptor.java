package com.bitian.superquery;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONValidator;
import com.bitian.common.dto.BaseForm;
import com.bitian.common.dto.QueryGroup;
import com.bitian.common.dto.QueryJoin;
import com.bitian.common.dto.SubQuery;
import com.bitian.common.enums.QueryConditionType;
import com.bitian.common.enums.SuperQueryCondition;
import com.bitian.common.enums.SuperQueryType;
import com.bitian.common.exception.CustomException;
import com.bitian.common.util.PrimaryKeyUtil;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author admin
 */
@Intercepts({
        @Signature(type = Executor.class, method = "queryCursor", args = { MappedStatement.class,Object.class, RowBounds.class}),
        @Signature(type = Executor.class, method = "query", args = { MappedStatement.class,Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = { MappedStatement.class,Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class SuperQueryInterceptor implements Interceptor {

    private MyProperties myProperties;

    public SuperQueryInterceptor(){
        this.myProperties=new MyProperties();
        this.myProperties.setEnable(true);
    }

    public SuperQueryInterceptor(MyProperties myProperties){
        this.myProperties=myProperties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object param=args[1];
        RowBounds rowBounds = (RowBounds) args[2];
        Executor executor = (Executor) invocation.getTarget();
        CacheKey cacheKey;
        BoundSql boundSql=null;
        BaseForm form=null;
        if( param instanceof BaseForm){
            form= (BaseForm) param;
        }else if(param instanceof Map){
            for (Object value : ((Map<?, ?>) param).values()) {
                if(value instanceof BaseForm){
                    form=((BaseForm) value);
                    break;
                }
            }
        }
        if(form==null){
            return invocation.proceed();
        }
        if(args.length == 3){
            if(form!=null && myProperties.getAutoAttach()==false){
                this.handleSql(form);
            }
            return invocation.proceed();
        } else if(args.length==4){
            //4 个参数时
            ResultHandler resultHandler = (ResultHandler) args[3];
            this.handleSql(form);
            boundSql = ms.getBoundSql(param);
            cacheKey = executor.createCacheKey(ms, param, rowBounds, boundSql);
            return executor.query(ms, param, rowBounds, resultHandler, cacheKey, boundSql);
        } else {
            //6 个参数时
            ResultHandler resultHandler = (ResultHandler) args[3];
            cacheKey = (CacheKey) args[4];
            boundSql = (BoundSql) args[5];
            this.handleSql(form);
            return executor.query(ms, param, rowBounds, resultHandler, cacheKey, boundSql);
        }

    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }

    private void handleSql(BaseForm form) throws Exception {
        Map<String,Object> map=new HashMap<>();
        String sql=this.parseCondition(form.get_groups(),map);
        form.set_sql(sql);
        form.set_sql_data(map);
    }

    private String parseQuery(SubQuery subQuery,Map<String,Object> map) throws Exception {
        String sql="select "+StringUtils.join(subQuery.getColumns(),",")+" from "+subQuery.getName()+" ";
        if(subQuery.getJoins()!=null && subQuery.getJoins().size()>0){
            for (QueryJoin join : subQuery.getJoins()) {
                sql+=" left join "+join.getName()+" on "+this.parseCondition(join.getConditions(),map);
            }
        }
        sql+=" where "+this.parseCondition(subQuery.getConditions(),map);
        return sql;
    }

    private String parseCondition(List<QueryGroup> conditions,Map<String,Object> map) throws Exception {
        if(conditions.size()==0)
            return "";
        StringBuffer sb=new StringBuffer();
        for (int i = 0; i < conditions.size(); i++) {
            QueryGroup group=conditions.get(i);
            StringBuffer dt=new StringBuffer();
            for (int j = 0; j < group.getDetails().size(); j++) {
                QueryGroup.QueryDetail detail=group.getDetails().get(j);
                if(detail.getValue()==null||detail.getValue().toString().length()==0){
                    continue;
                }
                dt.append(" "+(j==0?"":detail.getCondition().toString())+" ");
                if(StringUtils.isNotBlank(detail.getAlias())){
                    dt.append(" "+detail.getAlias()+".");
                }
                if(detail.getDynamic()){
                    dt.append("data->>'$."+detail.getKey()+"' ");
                }else if(detail.getType()== SuperQueryType.exists){
                    dt.append(" ");
                }else{
                    dt.append(detail.getKey()+" ");
                }

                String key=detail.getKey()+"_"+ PrimaryKeyUtil.getUUID();

                switch (detail.getType()){
                    case eq:{
                        //等于
                        if(detail.getConditionType()== QueryConditionType.specificValue){
                            dt.append(" = #{_sql_data."+key +"}");
                            map.put(key,detail.getValue());
                        }else if(detail.getConditionType()== QueryConditionType.column){
                            dt.append(" = "+detail.getValue());
                        }else if(detail.getConditionType()== QueryConditionType.subQuery){
                            dt.append(" = "+this.parseQuery((SubQuery) detail.getValue(),map));
                        }
                        break;
                    }
                    case ne:{
                        //不等于
                        if(detail.getConditionType()== QueryConditionType.specificValue){
                            dt.append(" != #{_sql_data."+key +"}");
                            map.put(key,detail.getValue());
                        }else if(detail.getConditionType()== QueryConditionType.column){
                            dt.append(" != "+detail.getValue());
                        }else if(detail.getConditionType()== QueryConditionType.subQuery){
                            dt.append(" != "+this.parseQuery((SubQuery) detail.getValue(),map));
                        }
                        break;
                    }
                    case in:{
                        //多值等于
                        if(detail.getConditionType()== QueryConditionType.specificValue){
                            List<?> strs=null;
                            if(detail.getValue() instanceof List){
                                strs= (List<?>) detail.getValue();
                            }else if(detail.getValue() instanceof String){
                                String value=detail.getValue().toString();
                                if(StringUtils.isNotBlank(value)){
                                    if(JSONValidator.from(value).validate()){
                                        strs= JSONArray.parseArray(value);
                                    }else{
                                        strs=Arrays.asList(StringUtils.split(value,"\n"));
                                    }
                                }
                            }
                            dt.append(" in (");
                            for (int k = 0; k < strs.size(); k++) {
                                dt.append("#{_sql_data."+key+k+"}");
                                if(i!=strs.size()-1){
                                    dt.append(",");
                                }
                                map.put(key+k,strs.get(k));
                            }
                            dt.append(")");
                        }else if(detail.getConditionType()== QueryConditionType.subQuery){
                            dt.append(" in ("+this.parseQuery((SubQuery) detail.getValue(),map)+")");
                        }
                        break;
                    }
                    case like:{
                        // like
                        dt.append(" like #{_sql_data."+key +"}");
                        map.put(key,"%"+detail.getValue()+"%");
                        break;
                    }
                    case lt:{
                        //小于
                        dt.append(" < #{_sql_data."+key +"}");
                        map.put(key,detail.getValue());
                        break;
                    }
                    case gt:{
                        //大于
                        dt.append(" > #{_sql_data."+key +"}");
                        map.put(key,detail.getValue());
                        break;
                    }
                    case lte:{
                        //小于等于
                        dt.append(" <= #{_sql_data."+key +"}");
                        map.put(key,detail.getValue());
                        break;
                    }
                    case gte:{
                        //大于等于
                        dt.append(" >= #{_sql_data."+key +"}");
                        map.put(key,detail.getValue());
                        break;
                    }
                    case exists:{
                        if(detail.getConditionType()== QueryConditionType.subQuery){
                            dt.append(" exists ("+this.parseQuery((SubQuery) detail.getValue(),map)+")");
                        }else{
                            throw new CustomException("参数异常");
                        }
                        break;
                    }
                }
            }
            if(dt.length()>0){
                sb.append(" "+(i==0?"":group.getCondition().toString())+" (");
                sb.append(dt);
                sb.append(" ) ");
            }
        }
        return sb.toString();
    }

}
