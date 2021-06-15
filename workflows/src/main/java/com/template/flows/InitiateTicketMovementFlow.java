package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilities;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.template.states.CustomTicket;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;


import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@InitiatingFlow
@StartableByRPC
@CordaSerializable
public class InitiateTicketMovementFlow extends FlowLogic<String> {
    private final String buyer;
    private final String issuer;
    private final StateRef assetReference;

    public InitiateTicketMovementFlow(String buyer, String issuer, String hash, int index) {
        this.buyer = buyer;
        this.issuer = issuer;
        this.assetReference = new StateRef(SecureHash.parse(hash), index);
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        AccountInfo issuerAccountInfo = UtilitiesKt.getAccountService(this)
                .accountInfo(issuer).get(0).getState().getData();

        AnonymousParty issuerAccount = subFlow(new RequestKeyForAccount(issuerAccountInfo));

        AccountInfo receiverAccountInfo = UtilitiesKt.getAccountService(this)
                .accountInfo(buyer).get(0).getState().getData();

        AnonymousParty buyerAccount = subFlow(new RequestKeyForAccount(receiverAccountInfo));

        QueryCriteria.VaultQueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria()
                .withStatus(Vault.StateStatus.UNCONSUMED)
                .withStateRefs(ImmutableList.of(assetReference));

        StateAndRef<CustomTicket> ticketStateStateAndRef = getServiceHub().getVaultService()
                .queryBy(CustomTicket.class, queryCriteria).getStates().get(0);

        CustomTicket ticketState = ticketStateStateAndRef.getState().getData();

        TransactionBuilder txBuilder = new TransactionBuilder(notary);

        MoveTokensUtilities.addMoveNonFungibleTokens(txBuilder, getServiceHub(),
                ticketState.toPointer(CustomTicket.class), receiverAccountInfo.getHost());

        FlowSession buyerSession = initiateFlow(receiverAccountInfo.getHost());
        buyerSession.send(ticketState.getValuation());

        List<StateAndRef<FungibleToken>> inputs = subFlow(new ReceiveStateAndRefFlow<>(buyerSession));

        List<FungibleToken> moneyReceived = buyerSession.receive(List.class).unwrap(value -> value);
        System.out.println("moneyReceived: "+moneyReceived);

        MoveTokensUtilities.addMoveTokens(txBuilder, inputs, moneyReceived);

        SignedTransaction selfSignedTransaction = getServiceHub()
                .signInitialTransaction(txBuilder, ImmutableList.of(issuerAccountInfo.getHost().getOwningKey()));

        System.out.println("selfSignedTransaction: "+selfSignedTransaction);

        SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(
                selfSignedTransaction, ImmutableList.of(buyerSession), Collections.singleton(issuerAccountInfo.getHost().getOwningKey())));

        SignedTransaction stx = subFlow(new FinalityFlow(
                signedTransaction, ImmutableList.of(buyerSession)));
        subFlow(new UpdateDistributionListFlow(stx));

        return "\nTicket is sold to "+ buyer;
    }
}