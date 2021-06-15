package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount;
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

import java.util.Collections;

@InitiatingFlow
@StartableByRPC
public class CreateAccountFlow extends FlowLogic<String> {

    private final String accountName;
//    private final Party companyName;

    public CreateAccountFlow(String accountName) {
        this.accountName = accountName;
//        this.companyName = companyName;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        try {
            StateAndRef<AccountInfo> accountInfoStateAndRef = (StateAndRef<AccountInfo>) subFlow(new com.r3.corda.lib.accounts.workflows.flows.CreateAccount(accountName));
            subFlow(new ShareAccountInfo(accountInfoStateAndRef, Collections.singletonList(getOurIdentity())));
            return "Created account for "+accountName;
        } catch (Exception exp) {
            return "Account creation failed";
        }
    }
}
