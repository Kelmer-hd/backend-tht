package api_backend_tht.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
        // Configurar tamaños máximos según sea necesario
        partReader.setMaxParts(1);
        partReader.setMaxDiskUsagePerPart(10 * 1024 * 1024); // 10MB
        partReader.setMaxInMemorySize(10 * 1024 * 1024); // 10MB

        MultipartHttpMessageReader multipartReader = new MultipartHttpMessageReader(partReader);
        configurer.customCodecs().register(multipartReader);
    }
}