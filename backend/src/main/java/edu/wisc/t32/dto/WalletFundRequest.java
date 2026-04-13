package edu.wisc.t32.dto;

import edu.wisc.t32.controller.WalletController;

/**
 * A fund request object for the {@link WalletController}.
 */
public class WalletFundRequest {
  private float amount;

  public float getAmount() {
    return amount;
  }

  public void setAmount(float amount) {
    this.amount = amount;
  }
}
