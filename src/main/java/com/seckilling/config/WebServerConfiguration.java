package com.seckilling.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * This bean will be loaded in Spring container when there is no TomcatEmbeddedServletContainerFactory bean in it.
 */
@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        // customize tomcat connector
//        ((TomcatServletWebServerFactory) factory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
//            @Override
//            public void customize(Connector connector) {
//                Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
//                protocol.setKeepAliveTimeout(30000);
//                protocol.setMaxKeepAliveRequests(10000);
//            }
//        });

        ((TomcatServletWebServerFactory) factory).addConnectorCustomizers((Connector connector) -> {
                Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                //close keep-alive connection when no request in 30 secs
                protocol.setKeepAliveTimeout(30000);
                //close keep-alive connection when client sends more than 10000 requests
                protocol.setMaxKeepAliveRequests(10000);
        });

    }

}
