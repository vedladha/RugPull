package com.example.demo.repository;

import com.example.demo.model.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing {@link UserWallet} entities.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations for user wallets.
 */
public interface UserWalletRepository extends JpaRepository<UserWallet, Integer> {
}
