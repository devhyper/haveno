/*
 * This file is part of Haveno.
 *
 * Haveno is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Haveno is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Haveno. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.components.paymentmethods;

import bisq.desktop.util.validation.WeChatPayValidator;

import bisq.core.account.witness.AccountAgeWitnessService;
import bisq.core.locale.Res;
import bisq.core.payment.PaymentAccount;
import bisq.core.payment.WeChatPayAccount;
import bisq.core.payment.payload.PaymentAccountPayload;
import bisq.core.payment.payload.WeChatPayAccountPayload;
import bisq.core.util.coin.CoinFormatter;
import bisq.core.util.validation.InputValidator;

import javafx.scene.layout.GridPane;

import static bisq.desktop.util.FormBuilder.addCompactTopLabelTextFieldWithCopyIcon;

public class WeChatPayForm extends GeneralAccountNumberForm {

    private final WeChatPayAccount weChatPayAccount;

    public static int addFormForBuyer(GridPane gridPane, int gridRow, PaymentAccountPayload paymentAccountPayload) {
        addCompactTopLabelTextFieldWithCopyIcon(gridPane, ++gridRow, Res.get("payment.account.no"), ((WeChatPayAccountPayload) paymentAccountPayload).getAccountNr());
        return gridRow;
    }

    public WeChatPayForm(PaymentAccount paymentAccount, AccountAgeWitnessService accountAgeWitnessService, WeChatPayValidator weChatPayValidator, InputValidator inputValidator, GridPane gridPane, int gridRow, CoinFormatter formatter) {
        super(paymentAccount, accountAgeWitnessService, inputValidator, gridPane, gridRow, formatter);
        this.weChatPayAccount = (WeChatPayAccount) paymentAccount;
    }

    @Override
    void setAccountNumber(String newValue) {
        weChatPayAccount.setAccountNr(newValue);
    }

    @Override
    String getAccountNr() {
        return weChatPayAccount.getAccountNr();
    }
}
