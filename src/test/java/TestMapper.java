import com.alibaba.fastjson.JSONObject;
import com.bitian.common.dto.BaseForm;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author admin
 */
public interface TestMapper {

    @Select("select * from sys_user su where ${_sql}")
    List<JSONObject> query(BaseForm form);

}
