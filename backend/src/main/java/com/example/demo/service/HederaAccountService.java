package com.example.demo.service;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HederaAccountService {
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^0\\.0\\.\\d+$");

    @Value("${hedera.operator.id:}")
    private String operatorId;

    @Value("${hedera.operator.key:}")
    private String operatorKey;

    @Value("${hedera.network:testnet}")
    private String network;

    @Value("${hedera.new-account.initial-hbar:10}")
    private long initialHbar;

    public String createAccountId() {
        if (operatorId == null
                || operatorId.isBlank()
                || operatorKey == null
                || operatorKey.isBlank()) {
            throw new IllegalStateException(
                    "Hedera operator credentials are missing. Set HEDERA_OPERATOR_ID and"
                            + " HEDERA_OPERATOR_KEY.");
        }
        if (!ACCOUNT_ID_PATTERN.matcher(operatorId.trim()).matches()) {
            throw new IllegalStateException(
                    "HEDERA_OPERATOR_ID must be in the format 0.0.<number>.");
        }

        try (Client client = createClientForNetwork()) {
            AccountId parsedOperatorId = AccountId.fromString(operatorId.trim());
            PrivateKey parsedOperatorKey = parseOperatorKey(operatorKey.trim());
            PrivateKey newAccountKey = PrivateKey.generateECDSA();
            client.setOperator(parsedOperatorId, parsedOperatorKey);
            var tx = new AccountCreateTransaction()
                    .setKeyWithAlias(newAccountKey.getPublicKey())
                    .setInitialBalance(new Hbar(initialHbar))
                    .freezeWith(client)
                    .sign(parsedOperatorKey);
            var response = tx.execute(client);
            var receipt = response.getReceipt(client);

            if (receipt.accountId == null) {
                throw new IllegalStateException(
                        "Hedera account was not created: receipt account ID is null.");
            }

            return receipt.accountId.toString();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create Hedera account: "
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage(),
                    e);
        }
    }

    private Client createClientForNetwork() {
        return switch (network.toLowerCase(Locale.ROOT)) {
            case "mainnet" -> Client.forMainnet();
            case "previewnet" -> Client.forPreviewnet();
            default -> Client.forTestnet();
        };
    }

    private PrivateKey parseOperatorKey(String rawKey) {
        String normalized = rawKey.startsWith("0x") ? rawKey.substring(2) : rawKey;
        try {
            return PrivateKey.fromStringECDSA(normalized);
        } catch (Exception ignored) {
            try {
                return PrivateKey.fromString(normalized);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "HEDERA_OPERATOR_KEY is invalid. Expected a valid private key string.", e);
            }
        }
    }
}
