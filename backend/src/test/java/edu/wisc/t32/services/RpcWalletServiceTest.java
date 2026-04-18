package edu.wisc.t32.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.hashgraph.sdk.PrivateKey;
import edu.wisc.t32.api.TransferResponse;
import edu.wisc.t32.api.Wallet;
import edu.wisc.t32.api.WalletService;
import edu.wisc.t32.model.UserWallet;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

class RpcWalletServiceTest {
  @Test
  void transferToOperator_usesConfiguredOperatorWalletWithoutReconstructingItThroughWalletService()
      throws Exception {
    RpcWalletService rpcWalletService = new RpcWalletService();
    UserWallet userWallet = buildUserWallet();
    String operatorId = "0.0.5005";
    String operatorKey = PrivateKey.generateECDSA().toString();
    String currencyId = "0.0.6006";

    ReflectionTestUtils.setField(rpcWalletService, "operatorId", operatorId);
    ReflectionTestUtils.setField(rpcWalletService, "operatorKey", operatorKey);
    ReflectionTestUtils.setField(rpcWalletService, "currencyId", currencyId);

    WalletService walletService = mock(WalletService.class);
    Wallet userRpcWallet = mock(Wallet.class);
    when(walletService.createWallet("0.0.7007", "user-private-key")).thenReturn(userRpcWallet);
    when(walletService.transferBalance(any(Wallet.class), any(Wallet.class), anyFloat()))
        .thenReturn(TransferResponse.SUCCESS);

    try (MockedStatic<WalletService> walletServiceFactory = mockStatic(WalletService.class)) {
      walletServiceFactory.when(() -> WalletService.getService(operatorId, operatorKey, currencyId))
          .thenReturn(walletService);

      rpcWalletService.transferToOperator(userWallet, 10.00f);
    }

    verify(walletService).transferBalance(
        eq(userRpcWallet),
        argThat(wallet -> operatorId.equals(wallet.getWalletId())),
        eq(10.00f)
    );
    verify(walletService, times(1)).createWallet(any(), any());
  }

  @Test
  void transferFromOperator_usesConfiguredOperatorWalletAsTheSender() throws Exception {
    RpcWalletService rpcWalletService = new RpcWalletService();
    UserWallet userWallet = buildUserWallet();
    String operatorId = "0.0.5005";
    String operatorKey = PrivateKey.generateECDSA().toString();
    String currencyId = "0.0.6006";

    ReflectionTestUtils.setField(rpcWalletService, "operatorId", operatorId);
    ReflectionTestUtils.setField(rpcWalletService, "operatorKey", operatorKey);
    ReflectionTestUtils.setField(rpcWalletService, "currencyId", currencyId);

    WalletService walletService = mock(WalletService.class);
    Wallet userRpcWallet = mock(Wallet.class);
    when(walletService.createWallet("0.0.7007", "user-private-key")).thenReturn(userRpcWallet);
    when(walletService.transferBalance(any(Wallet.class), any(Wallet.class), anyFloat()))
        .thenReturn(TransferResponse.SUCCESS);

    try (MockedStatic<WalletService> walletServiceFactory = mockStatic(WalletService.class)) {
      walletServiceFactory.when(() -> WalletService.getService(operatorId, operatorKey, currencyId))
          .thenReturn(walletService);

      rpcWalletService.transferFromOperator(userWallet, 12.50f);
    }

    verify(walletService).transferBalance(
        argThat(wallet -> operatorId.equals(wallet.getWalletId())),
        eq(userRpcWallet),
        eq(12.50f)
    );
    verify(walletService, times(1)).createWallet(any(), any());
  }

  private UserWallet buildUserWallet() {
    UserWallet wallet = new UserWallet();
    wallet.setUserId(7);
    wallet.setWalletAddress("0.0.7007");
    wallet.setWalletPrivateKey("user-private-key");
    return wallet;
  }
}
