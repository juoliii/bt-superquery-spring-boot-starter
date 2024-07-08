package com.bitian.superquery;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author admin
 */
@ConfigurationProperties(prefix = "bt.superquery")
@Data
public class MyProperties {

    private Boolean enable=false;

    private Boolean autoAttach=false;

}
