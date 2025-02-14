package com.aivle.TermCompass.repository;

import com.aivle.TermCompass.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByLink(String name);
    List<Company> findAll();
}
