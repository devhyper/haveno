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

package bisq.desktop.main.funds.transactions;

import bisq.core.btc.wallet.XmrWalletService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DisplayedTransactionsFactory {
    private final XmrWalletService xmrWalletService;
    private final TradableRepository tradableRepository;
    private final TransactionListItemFactory transactionListItemFactory;
    private final TransactionAwareTradableFactory transactionAwareTradableFactory;

    @Inject
    DisplayedTransactionsFactory(XmrWalletService xmrWalletService,
                                 TradableRepository tradableRepository,
                                 TransactionListItemFactory transactionListItemFactory,
                                 TransactionAwareTradableFactory transactionAwareTradableFactory) {
        this.xmrWalletService = xmrWalletService;
        this.tradableRepository = tradableRepository;
        this.transactionListItemFactory = transactionListItemFactory;
        this.transactionAwareTradableFactory = transactionAwareTradableFactory;
    }

    DisplayedTransactions create() {
        return new DisplayedTransactions(xmrWalletService, tradableRepository, transactionListItemFactory,
                transactionAwareTradableFactory);
    }
}
