package com.acmeair.morphia.services;

import com.acmeair.morphia.MorphiaConstants;
import com.acmeair.service.DataService;
import com.acmeair.service.TransactionService;

@DataService(name=MorphiaConstants.KEY,description=MorphiaConstants.KEY_DESCRIPTION)
public class TransactionServiceImpl implements TransactionService {

    @Override
    public void prepareForTransaction() throws Exception {
        // Simple implementation - no transaction handling needed for MongoDB
        // MongoDB operations are atomic at document level
    }
}