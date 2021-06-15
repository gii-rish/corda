package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.selection.api.Selector;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Currency;
import java.util.List;

@InitiatedBy(InitiateTicketMovementFlow.class)
public class ResponderTicketFlow extends FlowLogic<Void> {
    private final FlowSession otherPartySession;

    public ResponderTicketFlow(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {

        Amount<Currency> price =  otherPartySession.receive(Amount.class).unwrap(amount -> amount);

        Amount<TokenType> priceToken = new Amount<>(price.getQuantity(),
                FiatCurrency.Companion.getInstance(price.getToken().getCurrencyCode()));

        Selector selector = new DatabaseTokenSelection(getServiceHub());
        Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs = selector
                        .generateMove(Collections.singletonList(new Pair<>(otherPartySession.getCounterparty(), priceToken)), getOurIdentity());

        subFlow(new SendStateAndRefFlow(otherPartySession, inputsAndOutputs.getFirst()));
        otherPartySession.send(inputsAndOutputs.getSecond());

        System.out.println("Yes");
        SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(otherPartySession) {
            @Suspendable
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });
        System.out.println("otherPartySession from responder: "+otherPartySession);
        if(!otherPartySession.getCounterparty().equals(getOurIdentity()))
            subFlow(new ReceiveFinalityFlow(otherPartySession));
//        subFlow(new ReceiveFinalityFlow(otherPartySession, signedTransaction.getId()));
        return  null;
    }
}
