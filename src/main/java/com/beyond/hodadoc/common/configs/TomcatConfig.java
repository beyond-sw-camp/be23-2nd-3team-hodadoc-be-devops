package com.beyond.hodadoc.common.configs;

import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setMaxParameterCount(10000);  // 파라미터 수 제한
            connector.setMaxPartCount(1000);         // 파트(파일) 수 제한
        });
    }
}
