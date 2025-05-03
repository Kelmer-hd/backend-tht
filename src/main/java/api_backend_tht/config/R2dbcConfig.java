package api_backend_tht.config;

import api_backend_tht.converters.EstadoAlmacenTelaToStringConverter;
import api_backend_tht.converters.StringToEstadoAlmacenTelaConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

import java.util.Arrays;
import java.util.List;

@Configuration
public abstract class R2dbcConfig extends AbstractR2dbcConfiguration {
    // Otras configuraciones...

    @Override
    protected List<Object> getCustomConverters() {
        return Arrays.asList(
                new EstadoAlmacenTelaToStringConverter(),
                new StringToEstadoAlmacenTelaConverter()
                // Otros convertidores
        );
    }
}