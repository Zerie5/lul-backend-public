package com.lul.repository;

import com.lul.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Integer> {
    List<UserWallet> findByUserId(Long userId);
    Optional<UserWallet> findByUserIdAndWalletId(Long userId, Integer walletId);
    boolean existsByUserIdAndWalletId(Long userId, Integer walletId);
}
