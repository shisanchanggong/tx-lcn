/*
 * Copyright 2017-2019 CodingApi .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingapi.txlcn.tc.aspect;

import com.codingapi.txlcn.common.util.Maps;
import com.codingapi.txlcn.common.util.Transactions;
import com.codingapi.txlcn.tc.core.context.NonSpringRuntimeContext;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeEditor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Description: LCN based transaction metadata.
 * Date: 19-2-27 上午9:44
 *
 * @author ujued
 */
public class BranchNameMatchTransactionAttributeSource extends NameMatchTransactionAttributeSource {

    @Override
    public void setProperties(Properties transactionAttributes) {
        TransactionAttributeEditor tae = new TransactionAttributeEditor();
        Enumeration<?> propNames = transactionAttributes.propertyNames();
        while (propNames.hasMoreElements()) {
            String methodName = (String) propNames.nextElement();
            String value = transactionAttributes.getProperty(methodName);
            tae.setAsText(handleLcnTransactionAttributes(methodName, value));
            TransactionAttribute attr = (TransactionAttribute) tae.getValue();
            addTransactionalMethod(methodName, attr);
        }
    }

    private String handleLcnTransactionAttributes(String methodName, String value) {
        String[] tokens = StringUtils.commaDelimitedListToStringArray(value);
        return Arrays.stream(tokens).filter(token -> {
            boolean giveSpring = true;
            // resolve dtx metadata

            // transaction type. default is lcn
            String type = Transactions.LCN;
            if (token.startsWith("DTX_")) {
                token = token.substring(4);
                if (token.startsWith("TYPE_")) {
                    type = token.substring(5).toLowerCase();
                    Assert.isTrue(Transactions.VALID_TRANSACTION_TYPES.contains(type),
                            "non exists transaction type: " + type);
                }
                giveSpring = false;
            }
            NonSpringRuntimeContext.instance().cacheTransactionAttribute(
                    methodName, Maps.newHashMap(NonSpringRuntimeContext.TRANSACTION_TYPE, type));
            return giveSpring;
        }).collect(Collectors.joining(","));
    }
}