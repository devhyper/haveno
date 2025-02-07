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

package bisq.core.trade.protocol.tasks.buyer;

import bisq.core.btc.model.XmrAddressEntry;
import bisq.core.btc.wallet.XmrWalletService;
import bisq.core.network.MessageState;
import bisq.core.trade.Trade;
import bisq.core.trade.messages.PaymentSentMessage;
import bisq.core.trade.messages.TradeMailboxMessage;
import bisq.core.trade.messages.TradeMessage;
import bisq.core.trade.protocol.tasks.SendMailboxMessageTask;

import bisq.common.Timer;
import bisq.common.UserThread;
import bisq.common.taskrunner.TaskRunner;

import javafx.beans.value.ChangeListener;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import monero.wallet.MoneroWallet;

/**
 * We send the seller the BuyerSendPaymentSentMessage.
 * We wait to receive a ACK message back and resend the message
 * in case that does not happen in 10 minutes or if the message was stored in mailbox or failed. We keep repeating that
 * with doubling the interval each time and until the MAX_RESEND_ATTEMPTS is reached.
 * If never successful we give up and complete. It might be a valid case that the peer was not online for an extended
 * time but we can be very sure that our message was stored as mailbox message in the network and one the peer goes
 * online he will process it.
 */
@Slf4j
public class BuyerSendsPaymentSentMessage extends SendMailboxMessageTask {
    private static final int MAX_RESEND_ATTEMPTS = 10;
    private int delayInMin = 15;
    private int resendCounter = 0;
    private PaymentSentMessage message;
    private ChangeListener<MessageState> listener;
    private Timer timer;

    public BuyerSendsPaymentSentMessage(TaskRunner<Trade> taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected TradeMailboxMessage getTradeMailboxMessage(String tradeId) {
        if (message == null) {

            // gather relevant info
            XmrWalletService walletService = processModel.getProvider().getXmrWalletService();
            final String id = processModel.getOfferId();
            XmrAddressEntry payoutAddressEntry = walletService.getOrCreateAddressEntry(id, XmrAddressEntry.Context.TRADE_PAYOUT);

            // We do not use a real unique ID here as we want to be able to re-send the exact same message in case the
            // peer does not respond with an ACK msg in a certain time interval. To avoid that we get dangling mailbox
            // messages where only the one which gets processed by the peer would be removed we use the same uid. All
            // other data stays the same when we re-send the message at any time later.
            String deterministicId = tradeId + processModel.getMyNodeAddress().getFullAddress();
            message = new PaymentSentMessage(
                    tradeId,
                    payoutAddressEntry.getAddressString(),
                    processModel.getMyNodeAddress(),
                    trade.getCounterCurrencyTxId(),
                    trade.getCounterCurrencyExtraData(),
                    deterministicId,
                    trade.getBuyer().getPayoutTxHex(),
                    trade.getBuyer().getUpdatedMultisigHex()
            );
        }
        return message;
    }

    @Override
    protected void setStateSent() {
        if (trade.getState().ordinal() < Trade.State.BUYER_SENT_PAYMENT_SENT_MSG.ordinal()) {
            trade.setStateIfValidTransitionTo(Trade.State.BUYER_SENT_PAYMENT_SENT_MSG);
        }

        processModel.getTradeManager().requestPersistence();
    }

    @Override
    protected void setStateArrived() {
        // the message has arrived but we're ultimately waiting for an AckMessage response
        if (!trade.isPayoutPublished()) {
            tryToSendAgainLater();
        }
    }

    // We override the default behaviour for onStoredInMailbox and do not call complete
    @Override
    protected void onStoredInMailbox() {
        setStateStoredInMailbox();
    }

    @Override
    protected void setStateStoredInMailbox() {
        trade.setStateIfValidTransitionTo(Trade.State.BUYER_STORED_IN_MAILBOX_PAYMENT_SENT_MSG);
        if (!trade.isPayoutPublished()) {
            tryToSendAgainLater();
        }
        processModel.getTradeManager().requestPersistence();
    }

    // We override the default behaviour for onFault and do not call appendToErrorMessage and failed
    @Override
    protected void onFault(String errorMessage, TradeMessage message) {
        setStateFault();
    }

    @Override
    protected void setStateFault() {
        trade.setStateIfValidTransitionTo(Trade.State.BUYER_SEND_FAILED_PAYMENT_SENT_MSG);
        if (!trade.isPayoutPublished()) {
            tryToSendAgainLater();
        }
        processModel.getTradeManager().requestPersistence();
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();

            super.run();
        } catch (Throwable t) {
            failed(t);
        } finally {
            cleanup();
        }
    }

    // complete() is called from base class SendMailboxMessageTask=>onArrived()
    // We override the default behaviour for complete and keep this task open until receipt of the AckMessage
    @Override
    protected void complete() {
        onMessageStateChange(processModel.getPaymentStartedMessageStateProperty().get());  // check for AckMessage
    }

    private void cleanup() {
        if (timer != null) {
            timer.stop();
        }
        if (listener != null) {
            processModel.getPaymentStartedMessageStateProperty().removeListener(listener);
        }
    }

    private void tryToSendAgainLater() {
        if (resendCounter >= MAX_RESEND_ATTEMPTS) {
            cleanup();
            log.warn("We never received an ACK message when sending the PaymentSentMessage to the peer. " +
                    "We stop now and complete the protocol task.");
            complete();
            return;
        }

        log.info("We will send the message again to the peer after a delay of {} min.", delayInMin);
        if (timer != null) {
            timer.stop();
        }
        timer = UserThread.runAfter(this::run, delayInMin, TimeUnit.MINUTES);

        if (resendCounter == 0) {
            // We want to register listener only once
            listener = (observable, oldValue, newValue) -> onMessageStateChange(newValue);
            processModel.getPaymentStartedMessageStateProperty().addListener(listener);
            onMessageStateChange(processModel.getPaymentStartedMessageStateProperty().get());
        }

        delayInMin = delayInMin * 2;
        resendCounter++;
    }

    private void onMessageStateChange(MessageState newValue) {
        // Once we receive an ACK from our msg we know the peer has received the msg and we stop.
        if (newValue == MessageState.ACKNOWLEDGED) {
            // We treat a ACK like BUYER_SAW_ARRIVED_PAYMENT_SENT_MSG
            trade.setStateIfValidTransitionTo(Trade.State.BUYER_SAW_ARRIVED_PAYMENT_SENT_MSG);

            processModel.getTradeManager().requestPersistence();

            cleanup();
            super.complete();   // received AckMessage, complete this task
        }
    }
}
