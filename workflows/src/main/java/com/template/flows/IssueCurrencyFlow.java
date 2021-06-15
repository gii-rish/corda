package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.util.Arrays;

@StartableByRPC
public class IssueCurrencyFlow extends FlowLogic<String> {
    private final Long amount;
    private String currency;
    private final String accountName;

    public IssueCurrencyFlow(long amount, String currency, String accountName) {
        this.amount = amount;
        this.currency = currency;
        this.accountName = accountName;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        AccountInfo accountInfo = UtilitiesKt.getAccountService(this).
                accountInfo(accountName).get(0).getState().getData();

        AnonymousParty anonymousParty = subFlow(new RequestKeyForAccount(accountInfo));

//        Party anonymousParty = accountInfo.getHost();

//        TokenType tokenType = new TokenType("USD", 2);
        TokenType tokenType = FiatCurrency.Companion.getInstance(currency);

        IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), tokenType);

        FungibleToken fungibleToken = new FungibleToken(new Amount(this.amount, issuedTokenType), anonymousParty, null);

        SignedTransaction stx =subFlow(new IssueTokens(Arrays.asList(fungibleToken)));



//        TokenType tokenType = FiatCurrency.Companion.getInstance("USD");

//        FungibleToken fungibleToken =
//                new FungibleTokenBuilder()
//                        .ofTokenType(tokenType)
//                        .withAmount(amount)
//                        .issuedBy(getOurIdentity())
//                        .heldBy(anonymousParty)
//                        .buildFungibleToken();

        /* Issue the required amount of the token to the recipient */
//        subFlow(new IssueTokens(ImmutableList.of(fungibleToken), ImmutableList.of(anonymousParty)));
        return "Issued "+amount+".00 USD to "+accountName;
    }
}