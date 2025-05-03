package api_backend_tht.converters;

import api_backend_tht.model.entity.EstadoAlmacenTela;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class EstadoAlmacenTelaToStringConverter implements Converter<EstadoAlmacenTela, String> {
    @Override
    public String convert(EstadoAlmacenTela source) {
        return source.name();
    }
}