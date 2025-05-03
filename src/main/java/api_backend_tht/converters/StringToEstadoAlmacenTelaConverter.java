package api_backend_tht.converters;

import api_backend_tht.model.entity.EstadoAlmacenTela;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToEstadoAlmacenTelaConverter implements Converter<String, EstadoAlmacenTela> {
    @Override
    public EstadoAlmacenTela convert(String source) {
        return EstadoAlmacenTela.valueOf(source);
    }
}