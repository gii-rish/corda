package com.template.states;

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.template.contracts.TicketContract;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.ImmutableList;

import java.util.Currency;
import java.util.List;

@BelongsToContract(TicketContract.class)
public class CustomTicket extends EvolvableTokenType {
    private UniqueIdentifier tokenId;
    private final Party owner;
    private final String tokenType;
    private final Amount<Currency> valuation;

    public CustomTicket(UniqueIdentifier tokenId, Party owner, String tokenType, Amount<Currency> valuation) {
        this.tokenId = tokenId;
        this.owner = owner;
        this.tokenType = tokenType;
        this.valuation = valuation;
    }

    public UniqueIdentifier getTokenId() {
        return tokenId;
    }

    public Party getOwner() {
        return owner;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Amount<Currency> getValuation() {
        return valuation;
    }

    @Override
    public int getFractionDigits() {
        return 0;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return ImmutableList.of(this.getOwner());
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return tokenId;
    }

}
