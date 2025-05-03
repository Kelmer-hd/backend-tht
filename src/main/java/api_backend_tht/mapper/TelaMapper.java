package api_backend_tht.mapper;

import api_backend_tht.model.dto.TelaCreateDTO;
import api_backend_tht.model.dto.TelaDTO;
import api_backend_tht.model.entity.Tela;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TelaMapper {

    public TelaDTO toDto(Tela entity) {
        TelaDTO dto = new TelaDTO();
        dto.setId(entity.getId());
        dto.setNumGuia(entity.getNumGuia());
        dto.setPartida(entity.getPartida());
        dto.setOs(entity.getOs());
        dto.setProveedor(entity.getProveedor());
        dto.setFechaIngreso(entity.getFechaIngreso());
        dto.setCliente(entity.getCliente());
        dto.setMarca(entity.getMarca());
        dto.setOp(entity.getOp());
        dto.setTipoTela(entity.getTipoTela());
        dto.setDescripcion(entity.getDescripcion());
        dto.setEnch(entity.getEnch());
        dto.setCantRolloIngresado(entity.getCantRolloIngresado());
        dto.setPesoIngresado(entity.getPesoIngresado());
        dto.setStockReal(entity.getStockReal());
        dto.setEstado(entity.getEstado());
        dto.setAlmacen(entity.getAlmacen());

        return dto;
    }

    public Tela toEntity(TelaCreateDTO dto) {
        return Tela.builder()
                .numGuia(dto.getNumGuia())
                .partida(dto.getPartida())
                .os(dto.getOs())
                .proveedor(dto.getProveedor())
                .fechaIngreso(dto.getFechaIngreso())
                .cliente(dto.getCliente())
                .marca(dto.getMarca())
                .op(dto.getOp())
                .tipoTela(dto.getTipoTela())
                .descripcion(dto.getDescripcion())
                .ench(dto.getEnch())
                .cantRolloIngresado(dto.getCantRolloIngresado())
                .pesoIngresado(dto.getPesoIngresado())
                .stockReal(dto.getStockReal())
                .estado("ACTIVO")
                .almacen(dto.getAlmacen() != null ? dto.getAlmacen() : "PRINCIPAL")
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    public Tela updateEntityFromDto(Tela entity, TelaCreateDTO dto) {
        entity.setNumGuia(dto.getNumGuia());
        entity.setPartida(dto.getPartida());
        entity.setOs(dto.getOs());
        entity.setProveedor(dto.getProveedor());
        entity.setFechaIngreso(dto.getFechaIngreso());
        entity.setCliente(dto.getCliente());
        entity.setMarca(dto.getMarca());
        entity.setOp(dto.getOp());
        entity.setTipoTela(dto.getTipoTela());
        entity.setDescripcion(dto.getDescripcion());
        entity.setEnch(dto.getEnch());
        entity.setCantRolloIngresado(dto.getCantRolloIngresado());
        entity.setPesoIngresado(dto.getPesoIngresado());
        entity.setStockReal(dto.getStockReal());
        entity.setEstado(dto.getEstado());
        entity.setAlmacen(dto.getAlmacen());
        entity.setFechaActualizacion(LocalDateTime.now());

        return entity;
    }
}