 /*
  * Copyright 2010-2011 Ning, Inc.
  *
  * Ning licenses this file to you under the Apache License, version 2.0
  * (the "License"); you may not use this file except in compliance with the
  * License.  You may obtain a copy of the License at:
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
  * License for the specific language governing permissions and limitations
  * under the License.
  */
 package com.ning.billing.payment.core;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 import java.util.UUID;
 import java.util.concurrent.ExecutorService;
 
 import com.google.inject.Inject;
 import com.google.inject.name.Named;
 import com.ning.billing.ErrorCode;
 import com.ning.billing.account.api.Account;
 import com.ning.billing.account.api.AccountApiException;
 import com.ning.billing.account.api.AccountUserApi;
 import com.ning.billing.account.api.DefaultMutableAccountData;
 import com.ning.billing.account.api.MutableAccountData;
 import com.ning.billing.payment.api.DefaultPaymentMethod;
 import com.ning.billing.payment.api.DefaultPaymentMethodPlugin;
 import com.ning.billing.payment.api.PaymentApiException;
 import com.ning.billing.payment.api.PaymentMethod;
 import com.ning.billing.payment.api.PaymentMethodPlugin;
 import com.ning.billing.payment.dao.PaymentDao;
 import com.ning.billing.payment.dao.PaymentMethodModelDao;
 import com.ning.billing.payment.plugin.api.PaymentPluginApi;
 import com.ning.billing.payment.plugin.api.PaymentPluginApiException;
 import com.ning.billing.payment.provider.PaymentProviderPluginRegistry;
 import com.ning.billing.util.bus.Bus;
 import com.ning.billing.util.callcontext.CallContext;
 import com.ning.billing.util.globallocker.GlobalLocker;
 
 import static com.ning.billing.payment.glue.PaymentModule.PLUGIN_EXECUTOR_NAMED;
 
 public class PaymentMethodProcessor extends ProcessorBase {
 
     @Inject
     public PaymentMethodProcessor(final PaymentProviderPluginRegistry pluginRegistry,
                                   final AccountUserApi accountUserApi,
                                   final Bus eventBus,
                                   final PaymentDao paymentDao,
                                   final GlobalLocker locker,
                                   @Named(PLUGIN_EXECUTOR_NAMED) final ExecutorService executor) {
         super(pluginRegistry, accountUserApi, eventBus, paymentDao, locker, executor);
     }
 
     public Set<String> getAvailablePlugins() {
         return pluginRegistry.getRegisteredPluginNames();
     }
 
 
     public String initializeAccountPlugin(final String pluginName, final Account account) throws PaymentApiException {
 
         return new WithAccountLock<String>().processAccountWithLock(locker, account.getExternalKey(), new WithAccountLockCallback<String>() {
 
             @Override
             public String doOperation() throws PaymentApiException {
                 PaymentPluginApi pluginApi = null;
                 try {
                     // STEPH do we want to really have a default or fail?? probably fail
                     pluginApi = pluginRegistry.getPlugin(pluginName);
                     return pluginApi.createPaymentProviderAccount(account);
                 } catch (PaymentPluginApiException e) {
                     throw new PaymentApiException(ErrorCode.PAYMENT_PLUGIN_ACCOUNT_INIT,
                                                   account.getId(), pluginApi != null ? pluginApi.getName() : null, e.getErrorMessage());
                 }
             }
         });
     }
 
 
     public UUID addPaymentMethod(final String pluginName, final Account account,
                                  final boolean setDefault, final PaymentMethodPlugin paymentMethodProps, final CallContext context)
             throws PaymentApiException {
 
         return new WithAccountLock<UUID>().processAccountWithLock(locker, account.getExternalKey(), new WithAccountLockCallback<UUID>() {
 
             @Override
             public UUID doOperation() throws PaymentApiException {
                 PaymentMethod pm = null;
                 PaymentPluginApi pluginApi = null;
                 try {
                     pluginApi = pluginRegistry.getPlugin(pluginName);
                     pm = new DefaultPaymentMethod(account.getId(), pluginName, paymentMethodProps);
                     final String externalId = pluginApi.addPaymentMethod(account.getExternalKey(), paymentMethodProps, setDefault);
                     final PaymentMethodModelDao pmModel = new PaymentMethodModelDao(pm.getId(), pm.getAccountId(), pm.getPluginName(), pm.isActive(), externalId);
                     paymentDao.insertPaymentMethod(pmModel, context);
 
                     if (setDefault) {
                         final MutableAccountData updateAccountData = new DefaultMutableAccountData(account);
                         updateAccountData.setPaymentMethodId(pm.getId());
                         accountUserApi.updateAccount(account.getId(), updateAccountData, context);
                     }
                 } catch (PaymentPluginApiException e) {
                     // STEPH all errors should also take a pluginName
                     throw new PaymentApiException(ErrorCode.PAYMENT_ADD_PAYMENT_METHOD, account.getId(), e.getErrorMessage());
                 } catch (AccountApiException e) {
                     throw new PaymentApiException(e);
                 }
                 return pm.getId();
             }
         });
     }
 
 
     public List<PaymentMethod> refreshPaymentMethods(final String pluginName, final Account account, final CallContext context)
             throws PaymentApiException {
 
         return new WithAccountLock<List<PaymentMethod>>().processAccountWithLock(locker, account.getExternalKey(), new WithAccountLockCallback<List<PaymentMethod>>() {
 
             @Override
             public List<PaymentMethod> doOperation() throws PaymentApiException {
                 final List<PaymentMethod> result = new LinkedList<PaymentMethod>();
                final PaymentPluginApi pluginApi;
                 try {
                     pluginApi = pluginRegistry.getPlugin(pluginName);
                     final List<PaymentMethodPlugin> pluginPms = pluginApi.getPaymentMethodDetails(account.getExternalKey());
                    // The method should never return null by convention, but let's not trust the plugin...
                    if (pluginPms == null) {
                        return result;
                    }

                     for (final PaymentMethodPlugin cur : pluginPms) {
                         final PaymentMethod input = new DefaultPaymentMethod(account.getId(), pluginName, cur);
                         final PaymentMethodModelDao pmModel = new PaymentMethodModelDao(input.getId(), input.getAccountId(), input.getPluginName(), input.isActive(), input.getPluginDetail().getExternalPaymentMethodId());
                         // STEPH we should insert within one batch
                         paymentDao.insertPaymentMethod(pmModel, context);
                         result.add(input);
                     }
                 } catch (PaymentPluginApiException e) {
                     // STEPH all errors should also take a pluginName
                     throw new PaymentApiException(ErrorCode.PAYMENT_REFRESH_PAYMENT_METHOD, account.getId(), e.getErrorMessage());
                 }
                 return result;
             }
         });
     }
 
     public List<PaymentMethod> getPaymentMethods(final Account account, final boolean withPluginDetail) throws PaymentApiException {
 
         final List<PaymentMethodModelDao> paymentMethodModels = paymentDao.getPaymentMethods(account.getId());
         if (paymentMethodModels.size() == 0) {
             return Collections.emptyList();
         }
         return getPaymentMethodInternal(paymentMethodModels, account.getId(), account.getExternalKey(), withPluginDetail);
     }
 
     public PaymentMethod getPaymentMethodById(final UUID paymentMethodId)
             throws PaymentApiException {
         final PaymentMethodModelDao paymentMethodModel = paymentDao.getPaymentMethod(paymentMethodId);
         if (paymentMethodModel == null) {
             throw new PaymentApiException(ErrorCode.PAYMENT_NO_SUCH_PAYMENT_METHOD, paymentMethodId);
         }
         return new DefaultPaymentMethod(paymentMethodModel, null);
     }
 
     public PaymentMethod getPaymentMethod(final Account account, final UUID paymentMethodId, final boolean withPluginDetail)
             throws PaymentApiException {
         final PaymentMethodModelDao paymentMethodModel = paymentDao.getPaymentMethod(paymentMethodId);
         if (paymentMethodModel == null) {
             throw new PaymentApiException(ErrorCode.PAYMENT_NO_SUCH_PAYMENT_METHOD, paymentMethodId);
         }
         final List<PaymentMethod> result = getPaymentMethodInternal(Collections.singletonList(paymentMethodModel), account.getId(), account.getExternalKey(), withPluginDetail);
         return (result.size() == 0) ? null : result.get(0);
     }
 
 
     private List<PaymentMethod> getPaymentMethodInternal(final List<PaymentMethodModelDao> paymentMethodModels, final UUID accountId, final String accountKey, final boolean withPluginDetail)
             throws PaymentApiException {
 
         final List<PaymentMethod> result = new ArrayList<PaymentMethod>(paymentMethodModels.size());
         PaymentPluginApi pluginApi = null;
         try {
             List<PaymentMethodPlugin> pluginDetails = null;
             for (final PaymentMethodModelDao cur : paymentMethodModels) {
 
                 if (withPluginDetail) {
                     pluginApi = pluginRegistry.getPlugin(cur.getPluginName());
                     pluginDetails = pluginApi.getPaymentMethodDetails(accountKey);
                 }
 
                 final PaymentMethod pm = new DefaultPaymentMethod(cur, getPaymentMethodDetail(pluginDetails, cur.getExternalId()));
                 result.add(pm);
             }
         } catch (PaymentPluginApiException e) {
             throw new PaymentApiException(ErrorCode.PAYMENT_GET_PAYMENT_METHODS, accountId, e.getErrorMessage());
         }
         return result;
     }
 
 
     private PaymentMethodPlugin getPaymentMethodDetail(final List<PaymentMethodPlugin> pluginDetails, final String externalId) {
         if (pluginDetails == null) {
             return null;
         }
         for (final PaymentMethodPlugin cur : pluginDetails) {
             if (cur.getExternalPaymentMethodId().equals(externalId)) {
                 return cur;
             }
         }
         return null;
     }
 
     public void updatePaymentMethod(final Account account, final UUID paymentMethodId,
                                     final PaymentMethodPlugin paymentMethodProps)
             throws PaymentApiException {
 
         new WithAccountLock<Void>().processAccountWithLock(locker, account.getExternalKey(), new WithAccountLockCallback<Void>() {
 
             @Override
             public Void doOperation() throws PaymentApiException {
                 final PaymentMethodModelDao paymentMethodModel = paymentDao.getPaymentMethod(paymentMethodId);
                 if (paymentMethodModel == null) {
                     throw new PaymentApiException(ErrorCode.PAYMENT_NO_SUCH_PAYMENT_METHOD, paymentMethodId);
                 }
 
                 try {
                     final PaymentMethodPlugin inputWithId = new DefaultPaymentMethodPlugin(paymentMethodProps, paymentMethodModel.getExternalId());
                     final PaymentPluginApi pluginApi = getPluginApi(paymentMethodId, account.getId());
                     pluginApi.updatePaymentMethod(account.getExternalKey(), inputWithId);
                     return null;
                 } catch (PaymentPluginApiException e) {
                     throw new PaymentApiException(ErrorCode.PAYMENT_UPD_PAYMENT_METHOD, account.getId(), e.getErrorMessage());
                 }
             }
         });
     }
 
 
     public void deletedPaymentMethod(final Account account, final UUID paymentMethodId)
             throws PaymentApiException {
 
         new WithAccountLock<Void>().processAccountWithLock(locker, account.getExternalKey(), new WithAccountLockCallback<Void>() {
 
             @Override
             public Void doOperation() throws PaymentApiException {
                 final PaymentMethodModelDao paymentMethodModel = paymentDao.getPaymentMethod(paymentMethodId);
                 if (paymentMethodModel == null) {
                     throw new PaymentApiException(ErrorCode.PAYMENT_NO_SUCH_PAYMENT_METHOD, paymentMethodId);
                 }
 
                 try {
                     if (account.getPaymentMethodId().equals(paymentMethodId)) {
                         throw new PaymentApiException(ErrorCode.PAYMENT_DEL_DEFAULT_PAYMENT_METHOD, account.getId());
                     }
                     final PaymentPluginApi pluginApi = getPluginApi(paymentMethodId, account.getId());
                     pluginApi.deletePaymentMethod(account.getExternalKey(), paymentMethodModel.getExternalId());
                     paymentDao.deletedPaymentMethod(paymentMethodId);
                     return null;
                 } catch (PaymentPluginApiException e) {
                     throw new PaymentApiException(ErrorCode.PAYMENT_DEL_PAYMENT_METHOD, account.getId(), e.getErrorMessage());
                 }
             }
         });
     }
 
     public void setDefaultPaymentMethod(final Account account, final UUID paymentMethodId, final CallContext context)
             throws PaymentApiException {
 
         new WithAccountLock<Void>().processAccountWithLock(locker, account.getExternalKey(), new WithAccountLockCallback<Void>() {
 
             @Override
             public Void doOperation() throws PaymentApiException {
                 final PaymentMethodModelDao paymentMethodModel = paymentDao.getPaymentMethod(paymentMethodId);
                 if (paymentMethodModel == null) {
                     throw new PaymentApiException(ErrorCode.PAYMENT_NO_SUCH_PAYMENT_METHOD, paymentMethodId);
                 }
 
                 try {
                     final PaymentPluginApi pluginApi = getPluginApi(paymentMethodId, account.getId());
                     pluginApi.setDefaultPaymentMethod(account.getExternalKey(), paymentMethodModel.getExternalId());
                     final MutableAccountData updateAccountData = new DefaultMutableAccountData(account);
                     updateAccountData.setPaymentMethodId(paymentMethodId);
                     accountUserApi.updateAccount(account.getId(), updateAccountData, context);
                     return null;
                 } catch (PaymentPluginApiException e) {
                     throw new PaymentApiException(ErrorCode.PAYMENT_UPD_PAYMENT_METHOD, account.getId(), e.getErrorMessage());
                 } catch (AccountApiException e) {
                     throw new PaymentApiException(e);
                 }
             }
         });
     }
 
     private PaymentPluginApi getPluginApi(final UUID paymentMethodId, final UUID accountId)
             throws PaymentApiException {
         final PaymentMethodModelDao paymentMethod = paymentDao.getPaymentMethod(paymentMethodId);
         if (paymentMethod == null) {
             throw new PaymentApiException(ErrorCode.PAYMENT_NO_SUCH_PAYMENT_METHOD, paymentMethodId);
         }
         return pluginRegistry.getPlugin(paymentMethod.getPluginName());
     }
 }
