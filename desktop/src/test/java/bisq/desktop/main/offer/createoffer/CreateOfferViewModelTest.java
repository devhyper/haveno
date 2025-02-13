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

package bisq.desktop.main.offer.createoffer;

import bisq.desktop.util.validation.BtcValidator;
import bisq.desktop.util.validation.SecurityDepositValidator;

import bisq.core.account.witness.AccountAgeWitnessService;
import bisq.core.btc.model.XmrAddressEntry;
import bisq.core.btc.wallet.XmrWalletService;
import bisq.core.locale.Country;
import bisq.core.locale.CryptoCurrency;
import bisq.core.locale.GlobalSettings;
import bisq.core.locale.Res;
import bisq.core.offer.CreateOfferService;
import bisq.core.offer.OfferDirection;
import bisq.core.offer.OfferPayload;
import bisq.core.offer.OfferUtil;
import bisq.core.payment.PaymentAccount;
import bisq.core.payment.payload.PaymentMethod;
import bisq.core.provider.fee.FeeService;
import bisq.core.provider.price.MarketPrice;
import bisq.core.provider.price.PriceFeedService;
import bisq.core.trade.statistics.TradeStatisticsManager;
import bisq.core.user.Preferences;
import bisq.core.user.User;
import bisq.core.util.coin.CoinFormatter;
import bisq.core.util.coin.ImmutableCoinFormatter;
import bisq.core.util.validation.AltcoinValidator;
import bisq.core.util.validation.FiatPriceValidator;
import bisq.core.util.validation.InputValidator;

import bisq.common.config.Config;

import org.bitcoinj.core.Coin;

import javafx.beans.property.SimpleIntegerProperty;

import javafx.collections.FXCollections;

import java.time.Instant;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import static bisq.desktop.maker.PreferenceMakers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateOfferViewModelTest {

    private CreateOfferViewModel model;
    private final CoinFormatter coinFormatter = new ImmutableCoinFormatter(
            Config.baseCurrencyNetworkParameters().getMonetaryFormat());

    @Before
    public void setUp() {
        final CryptoCurrency btc = new CryptoCurrency("XMR", "monero");
        GlobalSettings.setDefaultTradeCurrency(btc);
        Res.setup();

        final BtcValidator btcValidator = new BtcValidator(coinFormatter);
        final AltcoinValidator altcoinValidator = new AltcoinValidator();
        final FiatPriceValidator fiatPriceValidator = new FiatPriceValidator();

        FeeService feeService = mock(FeeService.class);
        XmrAddressEntry addressEntry = mock(XmrAddressEntry.class);
        XmrWalletService xmrWalletService = mock(XmrWalletService.class);
        PriceFeedService priceFeedService = mock(PriceFeedService.class);
        User user = mock(User.class);
        PaymentAccount paymentAccount = mock(PaymentAccount.class);
        Preferences preferences = mock(Preferences.class);
        SecurityDepositValidator securityDepositValidator = mock(SecurityDepositValidator.class);
        AccountAgeWitnessService accountAgeWitnessService = mock(AccountAgeWitnessService.class);
        CreateOfferService createOfferService = mock(CreateOfferService.class);
        OfferUtil offerUtil = mock(OfferUtil.class);
        var tradeStats = mock(TradeStatisticsManager.class);

        when(xmrWalletService.getOrCreateAddressEntry(anyString(), any())).thenReturn(addressEntry);
        when(xmrWalletService.getBalanceForSubaddress(any(Integer.class))).thenReturn(Coin.valueOf(1000L));
        when(priceFeedService.updateCounterProperty()).thenReturn(new SimpleIntegerProperty());
        when(priceFeedService.getMarketPrice(anyString())).thenReturn(
                new MarketPrice("USD",
                        12684.0450,
                        Instant.now().getEpochSecond(),
                        true));
        when(feeService.getTxFee(anyInt())).thenReturn(Coin.valueOf(1000L));
        when(user.findFirstPaymentAccountWithCurrency(any())).thenReturn(paymentAccount);
        when(paymentAccount.getPaymentMethod()).thenReturn(mock(PaymentMethod.class));
        when(user.getPaymentAccountsAsObservable()).thenReturn(FXCollections.observableSet());
        when(securityDepositValidator.validate(any())).thenReturn(new InputValidator.ValidationResult(false));
        when(accountAgeWitnessService.getMyTradeLimit(any(), any(), any())).thenReturn(100000000L);
        when(preferences.getUserCountry()).thenReturn(new Country("ES", "Spain", null));
        when(createOfferService.getRandomOfferId()).thenReturn(UUID.randomUUID().toString());
        when(tradeStats.getObservableTradeStatisticsSet()).thenReturn(FXCollections.observableSet());

        CreateOfferDataModel dataModel = new CreateOfferDataModel(createOfferService,
            null,
            offerUtil,
            xmrWalletService,
            empty,
            user,
            null,
            priceFeedService,
            accountAgeWitnessService,
            feeService,
            coinFormatter,
            tradeStats,
            null);
        dataModel.initWithData(OfferDirection.BUY, new CryptoCurrency("XMR", "monero"));
        dataModel.activate();

        model = new CreateOfferViewModel(dataModel,
                null,
                fiatPriceValidator,
                altcoinValidator,
                btcValidator,
                securityDepositValidator,
                priceFeedService,
                null,
                null,
                preferences,
                coinFormatter,
                offerUtil);
        model.activate();
    }

    @Test
    public void testSyncMinAmountWithAmountUntilChanged() {
        assertNull(model.amount.get());
        assertNull(model.minAmount.get());

        model.amount.set("0.0");
        assertEquals("0.0", model.amount.get());
        assertNull(model.minAmount.get());

        model.amount.set("0.03");

        assertEquals("0.03", model.amount.get());
        assertEquals("0.03", model.minAmount.get());

        model.amount.set("0.0312");

        assertEquals("0.0312", model.amount.get());
        assertEquals("0.0312", model.minAmount.get());

        model.minAmount.set("0.01");
        model.onFocusOutMinAmountTextField(true, false);

        assertEquals("0.01", model.minAmount.get());

        model.amount.set("0.0301");

        assertEquals("0.0301", model.amount.get());
        assertEquals("0.01", model.minAmount.get());
    }

    @Test
    public void testSyncMinAmountWithAmountWhenZeroCoinIsSet() {
        model.amount.set("0.03");

        assertEquals("0.03", model.amount.get());
        assertEquals("0.03", model.minAmount.get());

        model.minAmount.set("0.00");
        model.onFocusOutMinAmountTextField(true, false);

        model.amount.set("0.04");

        assertEquals("0.04", model.amount.get());
        assertEquals("0.04", model.minAmount.get());

    }

    @Test
    public void testSyncMinAmountWithAmountWhenSameValueIsSet() {
        model.amount.set("0.03");

        assertEquals("0.03", model.amount.get());
        assertEquals("0.03", model.minAmount.get());

        model.minAmount.set("0.03");
        model.onFocusOutMinAmountTextField(true, false);

        model.amount.set("0.04");

        assertEquals("0.04", model.amount.get());
        assertEquals("0.04", model.minAmount.get());
    }

    @Test
    public void testSyncMinAmountWithAmountWhenHigherMinAmountValueIsSet() {
        model.amount.set("0.03");

        assertEquals("0.03", model.amount.get());
        assertEquals("0.03", model.minAmount.get());

        model.minAmount.set("0.05");
        model.onFocusOutMinAmountTextField(true, false);

        assertEquals("0.05", model.amount.get());
        assertEquals("0.05", model.minAmount.get());
    }

    @Test
    public void testSyncPriceMarginWithVolumeAndFixedPrice() {
        model.amount.set("0.01");
        model.onFocusOutPriceAsPercentageTextField(true, false); //leave focus without changing
        assertEquals("0.00", model.marketPriceMargin.get());
        assertEquals("0.00000078", model.volume.get());
        assertEquals("12684.04500000", model.price.get());
    }
}
