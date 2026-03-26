package edu.wisc.t32.repository;

import edu.wisc.t32.model.User;
import edu.wisc.t32.model.UserWallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing {@link UserWallet} entities.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations for user wallets.
 */
public interface UserWalletRepository extends JpaRepository<UserWallet, Integer> {
  /**
   * Retrieves the wallet associated with the specified user ID.
   *
   * <p>This method searches for a {@link UserWallet} linked to the provided userId.
   * It returns an {@link Optional} to safely handle cases where the user may not
   * have a wallet configured yet.
   *
   * @param userId the ID of the user whose wallet is being requested
   * @return an {@link Optional} containing the {@link UserWallet} if one exists,
   *     or an empty {@code Optional} if no wallet is found
   */
  Optional<UserWallet> findUserWalletByUserId(Integer userId);

}
