package com.example.demo.service;

import edu.wisc.t32.api.Wallet;
import edu.wisc.t32.api.WalletService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RpcWalletService {

    @Value("${wallet.operator.id:}")
    private String operatorId;

    @Value("${wallet.operator.key:}")
    private String operatorKey;

    @Value("${wallet.currency.id:}")
    private String currencyId;

    @Value("${wallet.new-account.initial-funding:10}")
    private int initialFunding;

    public WalletCredentials createWallet() {
        if (operatorId == null || operatorId.isBlank()) {
            throw new IllegalStateException("Wallet operator ID is missing. Set wallet.operator.id.");
        }
        if (operatorKey == null || operatorKey.isBlank()) {
            throw new IllegalStateException(
                    "Wallet operator key is missing. Set wallet.operator.key.");
        }
        if (currencyId == null || currencyId.isBlank()) {
            throw new IllegalStateException("Wallet currency ID is missing. Set wallet.currency.id.");
        }

        try (WalletService walletService =
                WalletService.getService(operatorId.trim(), operatorKey.trim(), currencyId.trim())) {
            Wallet wallet = walletService.createWallet(Math.max(0, initialFunding));
            if (wallet == null || wallet.getWalletId() == null || wallet.getWalletId().isBlank()) {
                throw new IllegalStateException(
                        "Wallet service did not return a valid wallet ID for the new user.");
            }
            if (wallet.getWalletPrivateKey() == null || wallet.getWalletPrivateKey().isBlank()) {
                throw new IllegalStateException(
                        "Wallet service did not return a private key for the new user.");
            }
            return new WalletCredentials(wallet.getWalletId(), wallet.getWalletPrivateKey());
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Failed to create wallet: "
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage(),
                    e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create wallet: " + e.getMessage(), e);
        }
    }

    public record WalletCredentials(String walletId, String walletPrivateKey) {}
}
