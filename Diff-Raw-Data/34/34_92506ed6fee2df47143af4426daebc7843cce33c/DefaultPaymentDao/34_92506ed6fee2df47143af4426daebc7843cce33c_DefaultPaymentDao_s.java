 /*
  * Copyright 2010-2013 Ning, Inc.
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
 
 package com.ning.billing.payment.dao;
 
 import java.util.Collection;
 import java.util.List;
 import java.util.UUID;
 
 import javax.inject.Inject;
 
 import org.joda.time.DateTime;
 import org.skife.jdbi.v2.IDBI;
 
 import com.ning.billing.payment.api.PaymentStatus;
 import com.ning.billing.payment.dao.RefundModelDao.RefundStatus;
 import com.ning.billing.util.cache.CacheControllerDispatcher;
 import com.ning.billing.util.callcontext.InternalCallContext;
 import com.ning.billing.util.callcontext.InternalTenantContext;
 import com.ning.billing.util.clock.Clock;
 import com.ning.billing.util.dao.NonEntityDao;
 import com.ning.billing.util.entity.EntityPersistenceException;
 import com.ning.billing.util.entity.dao.EntitySqlDao;
 import com.ning.billing.util.entity.dao.EntitySqlDaoTransactionWrapper;
 import com.ning.billing.util.entity.dao.EntitySqlDaoTransactionalJdbiWrapper;
 import com.ning.billing.util.entity.dao.EntitySqlDaoWrapperFactory;
 
 import com.google.common.base.Predicate;
 import com.google.common.collect.Collections2;
 
 public class DefaultPaymentDao implements PaymentDao {
 
     private final EntitySqlDaoTransactionalJdbiWrapper transactionalSqlDao;
 
     @Inject
     public DefaultPaymentDao(final IDBI dbi, final Clock clock, final CacheControllerDispatcher cacheControllerDispatcher, final NonEntityDao nonEntityDao) {
         this.transactionalSqlDao = new EntitySqlDaoTransactionalJdbiWrapper(dbi, clock, cacheControllerDispatcher, nonEntityDao);
     }
 
     @Override
     public PaymentAttemptModelDao insertNewAttemptForPayment(final UUID paymentId, final PaymentAttemptModelDao attempt, final InternalCallContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<PaymentAttemptModelDao>() {
             @Override
             public PaymentAttemptModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 final PaymentAttemptSqlDao transactional = entitySqlDaoWrapperFactory.become(PaymentAttemptSqlDao.class);
                 transactional.create(attempt, context);
                 final PaymentAttemptModelDao savedAttempt = transactional.getById(attempt.getId().toString(), context);
 
                 entitySqlDaoWrapperFactory.become(PaymentSqlDao.class).updatePaymentAmount(paymentId.toString(), savedAttempt.getRequestedAmount(), context);
 
                 return savedAttempt;
             }
         });
     }
 
     @Override
     public PaymentModelDao insertPaymentWithAttempt(final PaymentModelDao payment, final PaymentAttemptModelDao attempt, final InternalCallContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<PaymentModelDao>() {
 
             @Override
             public PaymentModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 final PaymentSqlDao transactional = entitySqlDaoWrapperFactory.become(PaymentSqlDao.class);
                 transactional.create(payment, context);
 
 
                 entitySqlDaoWrapperFactory.become(PaymentAttemptSqlDao.class).create(attempt, context);
 
                 return transactional.getById(payment.getId().toString(), context);
             }
         });
     }
 
     @Override
     public PaymentAttemptModelDao getPaymentAttempt(final UUID attemptId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<PaymentAttemptModelDao>() {
             @Override
             public PaymentAttemptModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(PaymentAttemptSqlDao.class).getById(attemptId.toString(), context);
             }
         });
     }
 
     @Override
     public void updateStatusAndEffectiveDateForPaymentWithAttempt(final UUID paymentId,
                                                                   final PaymentStatus paymentStatus,
                                                                   final DateTime newEffectiveDate,
                                                                   final UUID attemptId,
                                                                   final String gatewayErrorCode,
                                                                   final String gatewayErrorMsg,
                                                                   final InternalCallContext context) {
         transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<Void>() {
 
             @Override
             public Void inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 entitySqlDaoWrapperFactory.become(PaymentSqlDao.class).updatePaymentStatus(paymentId.toString(), paymentStatus.toString(), newEffectiveDate.toDate(), context);
                 entitySqlDaoWrapperFactory.become(PaymentAttemptSqlDao.class).updatePaymentAttemptStatus(attemptId.toString(), paymentStatus.toString(), gatewayErrorCode, gatewayErrorMsg, context);
                 return null;
             }
         });
     }
 
     @Override
     public PaymentMethodModelDao insertPaymentMethod(final PaymentMethodModelDao paymentMethod, final InternalCallContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<PaymentMethodModelDao>() {
             @Override
             public PaymentMethodModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return insertPaymentMethodInTransaction(entitySqlDaoWrapperFactory, paymentMethod, context);
             }
         });
     }
 
     private PaymentMethodModelDao insertPaymentMethodInTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory, final PaymentMethodModelDao paymentMethod, final InternalCallContext context)
             throws EntityPersistenceException {
         final PaymentMethodSqlDao transactional = entitySqlDaoWrapperFactory.become(PaymentMethodSqlDao.class);
         transactional.create(paymentMethod, context);
 
         return transactional.getById(paymentMethod.getId().toString(), context);
     }
 
     @Override
     public RefundModelDao insertRefund(final RefundModelDao refundInfo, final InternalCallContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<RefundModelDao>() {
 
             @Override
             public RefundModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 final RefundSqlDao transactional = entitySqlDaoWrapperFactory.become(RefundSqlDao.class);
                 transactional.create(refundInfo, context);
                 return transactional.getById(refundInfo.getId().toString(), context);
             }
         });
     }
 
     @Override
     public void updateRefundStatus(final UUID refundId, final RefundStatus refundStatus, final InternalCallContext context) {
         transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<Void>() {
             @Override
             public Void inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 entitySqlDaoWrapperFactory.become(RefundSqlDao.class).updateStatus(refundId.toString(), refundStatus.toString(), context);
                 return null;
             }
         });
     }
 
     @Override
     public RefundModelDao getRefund(final UUID refundId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<RefundModelDao>() {
             @Override
             public RefundModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(RefundSqlDao.class).getById(refundId.toString(), context);
             }
         });
     }
 
     @Override
     public List<RefundModelDao> getRefundsForPayment(final UUID paymentId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<RefundModelDao>>() {
             @Override
             public List<RefundModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(RefundSqlDao.class).getRefundsForPayment(paymentId.toString(), context);
             }
         });
     }
 
     @Override
     public List<RefundModelDao> getRefundsForAccount(final UUID accountId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<RefundModelDao>>() {
             @Override
             public List<RefundModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(RefundSqlDao.class).getRefundsForAccount(accountId.toString(), context);
             }
         });
     }
 
     @Override
     public PaymentMethodModelDao getPaymentMethod(final UUID paymentMethodId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<PaymentMethodModelDao>() {
             @Override
             public PaymentMethodModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(PaymentMethodSqlDao.class).getById(paymentMethodId.toString(), context);
             }
         });
     }
 
     @Override
     public PaymentMethodModelDao getPaymentMethodIncludedDeleted(final UUID paymentMethodId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<PaymentMethodModelDao>() {
             @Override
             public PaymentMethodModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(PaymentMethodSqlDao.class).getPaymentMethodIncludedDelete(paymentMethodId.toString(), context);
             }
         });
     }
 
     @Override
     public List<PaymentMethodModelDao> getPaymentMethods(final UUID accountId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<PaymentMethodModelDao>>() {
             @Override
             public List<PaymentMethodModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(PaymentMethodSqlDao.class).getByAccountId(accountId.toString(), context);
             }
         });
     }
 
     @Override
     public void deletedPaymentMethod(final UUID paymentMethodId, final InternalCallContext context) {
         transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<Void>() {
             @Override
             public Void inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 deletedPaymentMethodInTransaction(entitySqlDaoWrapperFactory, paymentMethodId, context);
                 return null;
             }
         });
     }
 
     private void deletedPaymentMethodInTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory, final UUID paymentMethodId, final InternalCallContext context) {
         entitySqlDaoWrapperFactory.become(PaymentMethodSqlDao.class).markPaymentMethodAsDeleted(paymentMethodId.toString(), context);
     }
 
     @Override
     public void undeletedPaymentMethod(final UUID paymentMethodId, final InternalCallContext context) {
         transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<Void>() {
             @Override
             public Void inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 undeletedPaymentMethodInTransaction(entitySqlDaoWrapperFactory, paymentMethodId, context);
                 return null;
             }
         });
     }
 
     private void undeletedPaymentMethodInTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory, final UUID paymentMethodId, final InternalCallContext context) {
         final PaymentMethodSqlDao paymentMethodSqlDao = entitySqlDaoWrapperFactory.become(PaymentMethodSqlDao.class);
         paymentMethodSqlDao.unmarkPaymentMethodAsDeleted(paymentMethodId.toString(), context);
     }
 
     @Override
     public List<PaymentModelDao> getPaymentsForInvoice(final UUID invoiceId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<PaymentModelDao>>() {
             @Override
             public List<PaymentModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(PaymentSqlDao.class).getPaymentsForInvoice(invoiceId.toString(), context);
             }
         });
     }
 
     @Override
     public PaymentModelDao getLastPaymentForPaymentMethod(final UUID accountId, final UUID paymentMethodId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<PaymentModelDao>() {
             @Override
             public PaymentModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(PaymentSqlDao.class).getLastPaymentForAccountAndPaymentMethod(accountId.toString(), paymentMethodId.toString(), context);
             }
         });
     }
 
     @Override
     public PaymentModelDao getPayment(final UUID paymentId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<PaymentModelDao>() {
             @Override
             public PaymentModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(PaymentSqlDao.class).getById(paymentId.toString(), context);
             }
         });
     }
 
     @Override
     public List<PaymentModelDao> getPaymentsForAccount(final UUID accountId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<PaymentModelDao>>() {
             @Override
             public List<PaymentModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(PaymentSqlDao.class).getPaymentsForAccount(accountId.toString(), context);
             }
         });
     }
 
     @Override
     public List<PaymentAttemptModelDao> getAttemptsForPayment(final UUID paymentId, final InternalTenantContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<PaymentAttemptModelDao>>() {
             @Override
             public List<PaymentAttemptModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 return entitySqlDaoWrapperFactory.become(PaymentAttemptSqlDao.class).getByPaymentId(paymentId.toString(), context);
             }
         });
     }
 
     @Override
     public List<PaymentMethodModelDao> refreshPaymentMethods(final UUID accountId, final String pluginName,
                                                              final List<PaymentMethodModelDao> newPaymentMethods, final InternalCallContext context) {
         return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<PaymentMethodModelDao>>() {
 
             @Override
             public List<PaymentMethodModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                 final PaymentMethodSqlDao transactional = entitySqlDaoWrapperFactory.become(PaymentMethodSqlDao.class);
                 // Look at all payment methods, including deleted ones. We assume that newPaymentMethods (payment methods returned by the plugin)
                 // is the full set of non-deleted payment methods in the plugin. If a payment method was marked as deleted on our side,
                 // but is still existing in the plugin, we will un-delete it.
                 final List<PaymentMethodModelDao> allPaymentMethodsForAccount = transactional.getByAccountIdIncludedDelete(accountId.toString(), context);
 
                 // Consider only the payment methods for the plugin we are refreshing
                 final Collection<PaymentMethodModelDao> existingPaymentMethods = Collections2.filter(allPaymentMethodsForAccount,
                                                                                                      new Predicate<PaymentMethodModelDao>() {
                                                                                                          @Override
                                                                                                          public boolean apply(final PaymentMethodModelDao paymentMethod) {
                                                                                                              return pluginName.equals(paymentMethod.getPluginName());
                                                                                                          }
                                                                                                      });
 
                 for (final PaymentMethodModelDao finalPaymentMethod : newPaymentMethods) {
                     PaymentMethodModelDao foundExistingPaymentMethod = null;
                     for (final PaymentMethodModelDao existingPaymentMethod : existingPaymentMethods) {
                         if (existingPaymentMethod.equals(finalPaymentMethod)) {
                             // We already have it - nothing to do
                             foundExistingPaymentMethod = existingPaymentMethod;
                             break;
                         } else if (existingPaymentMethod.equalsButActive(finalPaymentMethod)) {
                             // We already have it but its status has changed - update it accordingly
                             undeletedPaymentMethodInTransaction(entitySqlDaoWrapperFactory, existingPaymentMethod.getId(), context);
                             foundExistingPaymentMethod = existingPaymentMethod;
                             break;
                         }
                         // Otherwise, we don't have it
                     }
 
                     if (foundExistingPaymentMethod == null) {
                         insertPaymentMethodInTransaction(entitySqlDaoWrapperFactory, finalPaymentMethod, context);
                     } else {
                         existingPaymentMethods.remove(foundExistingPaymentMethod);
                     }
                 }
 
                 // Finally, all payment methods left in the existingPaymentMethods should be marked as deleted
                 for (final PaymentMethodModelDao existingPaymentMethod : existingPaymentMethods) {
                    deletedPaymentMethodInTransaction(entitySqlDaoWrapperFactory, existingPaymentMethod.getId(), context);
                 }
                 return transactional.getByAccountId(accountId.toString(), context);
             }
         });
     }
 }
