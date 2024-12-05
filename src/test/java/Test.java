import com.alibaba.fastjson.JSONObject;
import com.bitian.common.dto.BaseForm;
import com.bitian.common.dto.QueryGroup;
import com.bitian.common.dto.SubQuery;
import com.bitian.common.enums.QueryConditionType;
import com.bitian.common.enums.SuperQueryCondition;
import com.bitian.common.enums.SuperQueryType;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;

/**
 * @author admin
 */
public class Test {
    public static void main(String[] args) throws Exception {
        Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestMapper mapper = session.getMapper(TestMapper.class);
            BaseForm form=new BaseForm();
            QueryGroup group=new QueryGroup();
            QueryGroup.QueryDetail detail=new QueryGroup.QueryDetail();
            detail.setType(SuperQueryType.exists);
            detail.setConditionType(QueryConditionType.subQuery);
            SubQuery query=new SubQuery();
            query.setName("sys_user_sys_role");
            QueryGroup condition=new QueryGroup();
            QueryGroup.QueryDetail detail1=new QueryGroup.QueryDetail();
            detail1.setConditionType(QueryConditionType.column);
            detail1.setKey("sys_user_roles_id");
            detail1.setType(SuperQueryType.eq);
            detail1.setValue("su.id");
            QueryGroup.QueryDetail detail2=new QueryGroup.QueryDetail();
            detail2.setConditionType(QueryConditionType.specificValue);
            detail2.setKey("id");
            detail2.setValue(1);
            detail2.setType(SuperQueryType.eq);
            detail2.setCondition(SuperQueryCondition.and);
            condition.setDetails(Arrays.asList(detail1,detail2));
            query.setConditions(Arrays.asList(condition));
            detail.setValue(query);

            group.setDetails(Arrays.asList(detail));
            form.set_groups(Arrays.asList(group));
            form.setKey("管理员");
            List<JSONObject> list=mapper.query(form);
            System.out.println(list);
        }
    }
}
