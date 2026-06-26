package com.saas.MedStorage_api.returns.repository;

import com.saas.MedStorage_api.returns.entity.Return;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ReturnRepository extends JpaRepository<Return, UUID> {

    @Query(value = "select nextval('return_numero_seq')", nativeQuery = true)
    long nextNumeroRetornoSequence();
}
