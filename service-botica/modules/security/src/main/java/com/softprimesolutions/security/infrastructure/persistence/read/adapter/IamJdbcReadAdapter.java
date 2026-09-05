package com.softprimesolutions.security.infrastructure.persistence.read.adapter;

import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import com.softprimesolutions.security.infrastructure.persistence.read.mapper.IamReadMapper;
import com.softprimesolutions.security.infrastructure.persistence.read.repository.IamJdbcReadRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IamJdbcReadAdapter implements IamReadPort {

    private final IamJdbcReadRepository repository;

    public IamJdbcReadAdapter(IamJdbcReadRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<UsuarioResult> findUsers(UUID tenantId, String search, int page, int size) {
        var items = repository.findUsers(tenantId, search, page * size, size).stream()
                .map(IamReadMapper::toResult)
                .toList();
        return new PaginaResult<>(items, page, size, repository.countUsers(tenantId, search));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<RolResult> findRoles(UUID tenantId, String search, int page, int size) {
        var items = repository.findRoles(tenantId, search, page * size, size).stream()
                .map(IamReadMapper::toResult)
                .toList();
        return new PaginaResult<>(items, page, size, repository.countRoles(tenantId, search));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermisoResult> findPermissions(String search) {
        return repository.findPermissions(search).stream().map(IamReadMapper::toResult).toList();
    }
}
