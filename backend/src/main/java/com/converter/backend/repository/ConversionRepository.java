package com.converter.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.converter.backend.model.Conversion;

@Repository
public interface ConversionRepository extends JpaRepository<Conversion, Long> {

}
