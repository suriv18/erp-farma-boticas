package com.softprimesolutions.catalogo.infrastructure.persistence.read.adapter;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResumen;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.application.dto.result.SkuResumen;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.catalogo.infrastructure.persistence.read.repository.CatalogoJdbcReadRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogoJdbcReadAdapter implements CatalogoReadPort {

    private final CatalogoJdbcReadRepository repository;

    public CatalogoJdbcReadAdapter(CatalogoJdbcReadRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CondicionVentaResult> findCondicionesVenta(String estado) {
        return repository.findCondicionesVenta(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormaFarmaceuticaResult> findFormasFarmaceuticas(String estado) {
        return repository.findFormasFarmaceuticas(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViaAdministracionResult> findViasAdministracion(String estado) {
        return repository.findViasAdministracion(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnidadMedidaResult> findUnidadesMedida(String estado) {
        return repository.findUnidadesMedida(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClasificacionControladaResult> findClasificacionesControladas(String estado) {
        return repository.findClasificacionesControladas(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrincipioActivoResult> findPrincipiosActivos(String texto, String estado) {
        return repository.findPrincipiosActivos(texto, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<MarcaResult> findMarcas(UUID tenantId, String texto, String estado, int page, int size) {
        var items = repository.findMarcas(tenantId, texto, estado, page * size, size);
        var total = repository.countMarcas(tenantId, texto, estado);
        return new PaginaResult<>(items, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<CategoriaProductoResult> findCategoriasProducto(
            UUID tenantId, String texto, UUID categoriaPadreId, String estado, int page, int size) {
        var items = repository.findCategoriasProducto(tenantId, texto, categoriaPadreId, estado, page * size, size);
        var total = repository.countCategoriasProducto(tenantId, texto, categoriaPadreId, estado);
        return new PaginaResult<>(items, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<ProductoReguladoResumen> findProductosRegulados(
            String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size) {
        var items = repository.findProductosRegulados(texto, condicionVentaCodigo, estadoRegulatorio, page * size, size);
        var total = repository.countProductosRegulados(texto, condicionVentaCodigo, estadoRegulatorio);
        return new PaginaResult<>(items, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<SkuResumen> findSkus(
            UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado,
            int page, int size) {
        var items = repository.findSkus(tenantId, texto, categoriaId, marcaId, tipoSku, estado, page * size, size);
        var total = repository.countSkus(tenantId, texto, categoriaId, marcaId, tipoSku, estado);
        return new PaginaResult<>(items, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<RubroComercialResult> findRubrosComerciales(
            UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size) {
        var items = repository.findRubrosComerciales(tenantId, texto, esFarmaceutico, estado, page * size, size);
        var total = repository.countRubrosComerciales(tenantId, texto, esFarmaceutico, estado);
        return new PaginaResult<>(items, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<TipoDocumentoIdentidadResult> findTiposDocumentoIdentidad(String estado, int page, int size) {
        var items = repository.findTiposDocumentoIdentidad(estado, page * size, size);
        var total = repository.countTiposDocumentoIdentidad(estado);
        return new PaginaResult<>(items, page, size, total);
    }
}
