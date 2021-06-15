package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.template.states.CustomTicket;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.util.Arrays;
import java.util.Currency;
import java.util.UUID;

@StartableByRPC
public class CreateTicketFlow extends FlowLogic<String> {
    private final String type;
    private final Amount<Currency> value;
    private final String accountName;

    public CreateTicketFlow(String type, Amount<Currency> value, String accountName) {
        this.type = type;
        this.value = value;
        this.accountName = accountName;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        UniqueIdentifier uuid = UniqueIdentifier.Companion.fromString(UUID.randomUUID().toString());

        CustomTicket tokenType = new CustomTicket(uuid, getOurIdentity(), type, value);

        TransactionState transactionState = new TransactionState(tokenType, notary);
        subFlow(new CreateEvolvableTokens(transactionState));

        AccountInfo receiverAccountInfo = UtilitiesKt.getAccountService(this)
                .accountInfo(accountName).get(0).getState().getData();

        AnonymousParty receiverAccount = subFlow(new RequestKeyForAccount(receiverAccountInfo));

        IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(),
                tokenType.toPointer(tokenType.getClass()));

        NonFungibleToken nonFungibleToken = new NonFungibleToken(issuedTokenType, receiverAccount, new UniqueIdentifier(),
                TransactionUtilitiesKt.getAttachmentIdForGenericParam(tokenType.toPointer(tokenType.getClass())));
        SignedTransaction sgnTx = subFlow(new IssueTokens(Arrays.asList(nonFungibleToken)));

        return accountName+" created ticket of type "+type+" worth "+value;
    }
}
