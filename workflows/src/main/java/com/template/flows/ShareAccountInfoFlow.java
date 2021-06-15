package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount;
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class ShareAccountInfoFlow extends FlowLogic<String> {

    private final String accountName;
    private final List<Party> recipients;

    private final ProgressTracker progressTracker = new ProgressTracker();

    public ShareAccountInfoFlow(String accountName, List<Party> recipients) {
        this.accountName = accountName;
        this.recipients = recipients;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {

//        List<StateAndRef<AccountInfo>> allmyAccounts = getServiceHub()
//                .cordaService(KeyManagementBackedAccountService.class).ourAccounts();
//
//        StateAndRef<AccountInfo> SharedAccount = allmyAccounts.stream()
//                .filter(it -> it.getState().getData().getName().equals(accountName))
//                .findAny().get();
//
//        subFlow(new ShareAccountInfo(SharedAccount, partyToShareAccountInfoList));
//        return "Shared " + accountName + " with " + partyToShareAccountInfoList;

        AccountService accountService = UtilitiesKt.getAccountService(this);
        List<StateAndRef<AccountInfo>> accountInfoList = accountService.accountInfo(accountName);
        if(accountInfoList.size()==0){
            throw new FlowException("Account doesn't exist");
        }
        subFlow(new ShareAccountInfo(accountInfoList.get(0), recipients));
        return "" + accountName +" has been shared to " +recipients+".";

    }
}