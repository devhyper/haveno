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

import bisq.desktop.components.InputTextField;
import bisq.desktop.util.FormBuilder;
import bisq.desktop.util.Layout;
import bisq.desktop.util.validation.RevolutValidator;

import bisq.core.account.witness.AccountAgeWitnessService;
import bisq.core.locale.Res;
import bisq.core.payment.PaymentAccount;
import bisq.core.payment.RevolutAccount;
import bisq.core.payment.payload.PaymentAccountPayload;
import bisq.core.payment.payload.RevolutAccountPayload;
import bisq.core.util.coin.CoinFormatter;
import bisq.core.util.validation.InputValidator;

import bisq.common.util.Tuple2;

import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

import lombok.extern.slf4j.Slf4j;

import static bisq.desktop.util.FormBuilder.addCompactTopLabelTextField;
import static bisq.desktop.util.FormBuilder.addCompactTopLabelTextFieldWithCopyIcon;
import static bisq.desktop.util.FormBuilder.addTopLabelFlowPane;

@Slf4j
public class RevolutForm extends PaymentMethodForm {
    private final RevolutAccount account;
    private final RevolutValidator validator;

    public static int addFormForBuyer(GridPane gridPane, int gridRow,
                                      PaymentAccountPayload paymentAccountPayload) {
        Tuple2<String, String> tuple = ((RevolutAccountPayload) paymentAccountPayload).getRecipientsAccountData();
        addCompactTopLabelTextFieldWithCopyIcon(gridPane, ++gridRow, tuple.first, tuple.second);
        return gridRow;
    }

    public RevolutForm(PaymentAccount paymentAccount, AccountAgeWitnessService accountAgeWitnessService,
                       RevolutValidator revolutValidator, InputValidator inputValidator, GridPane gridPane,
                       int gridRow, CoinFormatter formatter) {
        super(paymentAccount, accountAgeWitnessService, inputValidator, gridPane, gridRow, formatter);
        this.account = (RevolutAccount) paymentAccount;
        this.validator = revolutValidator;
    }

    @Override
    public void addFormForAddAccount() {
        gridRowFrom = gridRow + 1;

        InputTextField userNameInputTextField = FormBuilder.addInputTextField(gridPane, ++gridRow, Res.get("payment.account.userName"));
        userNameInputTextField.setValidator(validator);
        userNameInputTextField.textProperty().addListener((ov, oldValue, newValue) -> {
            account.setUserName(newValue.trim());
            updateFromInputs();
        });

        addCurrenciesGrid(true);
        addLimitations(false);
        addAccountNameTextFieldWithAutoFillToggleButton();
    }

    private void addCurrenciesGrid(boolean isEditable) {
        FlowPane flowPane = addTopLabelFlowPane(gridPane, ++gridRow,
                Res.get("payment.supportedCurrencies"), Layout.FLOATING_LABEL_DISTANCE * 3,
                Layout.FLOATING_LABEL_DISTANCE * 3).second;

        if (isEditable)
            flowPane.setId("flow-pane-checkboxes-bg");
        else
            flowPane.setId("flow-pane-checkboxes-non-editable-bg");

        account.getSupportedCurrencies().forEach(e ->
                fillUpFlowPaneWithCurrencies(isEditable, flowPane, e, account));
    }

    @Override
    protected void autoFillNameTextField() {
        setAccountNameWithString(account.getUserName());
    }

    @Override
    public void addFormForEditAccount() {
        gridRowFrom = gridRow;
        addAccountNameTextFieldWithAutoFillToggleButton();
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("shared.paymentMethod"),
                Res.get(account.getPaymentMethod().getId()));

        String userName = account.getUserName();
        TextField userNameTf = addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("payment.account.userName"), userName).second;
        userNameTf.setMouseTransparent(false);

        if (account.hasOldAccountId()) {
            String accountId = account.getAccountId();
            TextField accountIdTf = addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("payment.account.phoneNr"), accountId).second;
            accountIdTf.setMouseTransparent(false);
        }

        addLimitations(true);
        addCurrenciesGrid(false);
    }

    @Override
    public void updateAllInputsValid() {
        allInputsValid.set(isAccountNameValid()
                && validator.validate(account.getUserName()).isValid
                && account.getTradeCurrencies().size() > 0);
    }
}
