import com.bitian.common.dto.BaseForm;

import java.util.List;

/**
 * @author admin
 */
public interface TestMapper {

    List<String> query(BaseForm form);

}
