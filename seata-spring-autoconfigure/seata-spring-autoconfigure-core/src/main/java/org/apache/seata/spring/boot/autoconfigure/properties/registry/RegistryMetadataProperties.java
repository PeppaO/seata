package org.apache.seata.spring.boot.autoconfigure.properties.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_METADATA_PREFIX;

@Component
@ConfigurationProperties(prefix = REGISTRY_METADATA_PREFIX)
public class RegistryMetadataProperties {
    private String external;

    public String getExternal() {
        return external;
    }

    public RegistryMetadataProperties setExternal(String external) {
        this.external = external;
        return this;
    }
}
