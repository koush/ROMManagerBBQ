package com.koushikdutta.rommanager.nomnom;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.os.Bundle;

public class SafeAccountHelper {
	private SafeAccountHelper() {
		
	}
	
    public static String[] getGoogleAccounts(Context context) {
        ArrayList<String> googleAccounts = new ArrayList<String>();
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            if (account.type.equals("com.google") && account.name.endsWith("gmail.com")) {
                googleAccounts.add(account.name);
            }
        }

        String[] result = new String[googleAccounts.size()];
        googleAccounts.toArray(result);
        return result;
    }	

    private static final String AUTH_TOKEN_TYPE = "ah";
    public static Bundle tryAuth(Context context, String accountName) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = new Account(accountName, "com.google");
        AccountManagerFuture<Bundle> future =
            accountManager.getAuthToken (account, AUTH_TOKEN_TYPE, false, null, null);
        Bundle bundle;
		try {
			bundle = future.getResult();
	        return bundle;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
    }

}
