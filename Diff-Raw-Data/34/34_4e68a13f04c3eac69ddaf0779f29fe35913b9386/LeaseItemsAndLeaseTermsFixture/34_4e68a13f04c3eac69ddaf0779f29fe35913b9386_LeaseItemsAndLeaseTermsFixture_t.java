 /*
  *
  *  Copyright 2012-2013 Eurocommercial Properties NV
  *
  *
  *  Licensed under the Apache License, Version 2.0 (the
  *  "License"); you may not use this file except in compliance
  *  with the License.  You may obtain a copy of the License at
  *
  *        http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing,
  *  software distributed under the License is distributed on an
  *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  *  KIND, either express or implied.  See the License for the
  *  specific language governing permissions and limitations
  *  under the License.
  */
 package org.estatio.fixture.lease;
 
 import java.math.BigDecimal;
 import java.math.BigInteger;
 
 import org.joda.time.LocalDate;
 
 import org.apache.isis.applib.fixtures.AbstractFixture;
 
 import org.estatio.dom.charge.Charge;
 import org.estatio.dom.charge.Charges;
 import org.estatio.dom.index.Index;
 import org.estatio.dom.index.Indices;
 import org.estatio.dom.invoice.PaymentMethod;
 import org.estatio.dom.lease.InvoicingFrequency;
 import org.estatio.dom.lease.Lease;
 import org.estatio.dom.lease.LeaseItem;
 import org.estatio.dom.lease.LeaseItemStatus;
 import org.estatio.dom.lease.LeaseItemType;
 import org.estatio.dom.lease.LeaseTerm;
 import org.estatio.dom.lease.LeaseTermForIndexableRent;
 import org.estatio.dom.lease.LeaseTermForServiceCharge;
 import org.estatio.dom.lease.LeaseTermForTurnoverRent;
 import org.estatio.dom.lease.LeaseTermFrequency;
 import org.estatio.dom.lease.LeaseTerms;
 import org.estatio.dom.lease.Leases;
 
 public class LeaseItemsAndLeaseTermsFixture extends AbstractFixture {
 
     @Override
     public void install() {
 
         Lease leaseTopModel = leases.findLeaseByReference("OXF-TOPMODEL-001");
         createLeaseTermForRent(
                 leaseTopModel,
                 leaseTopModel.getStartDate(),
                 null,
                 BigDecimal.valueOf(20000), new LocalDate(2010, 7, 1),
                 new LocalDate(2011, 1, 1),
                 new LocalDate(2011, 4, 1),
                 "ISTAT-FOI");
         createLeaseTermForServiceCharge(leaseTopModel, leaseTopModel.getStartDate(), null, BigDecimal.valueOf(6000));
         createLeaseTermForTurnoverRent(leaseTopModel, leaseTopModel.getStartDate().withDayOfYear(1).plusYears(1), null, "7");
 
         Lease leaseMediaX = leases.findLeaseByReference("OXF-MEDIAX-002");
         createLeaseTermForRent(leaseMediaX, leaseMediaX.getStartDate(), null, BigDecimal.valueOf(20000), new LocalDate(2008, 1, 1), new LocalDate(2009, 1, 1), new LocalDate(2009, 4, 1), "ISTAT-FOI");
         createLeaseTermForServiceCharge(leaseMediaX, leaseMediaX.getStartDate(), null, BigDecimal.valueOf(6000));
         createLeaseTermForTurnoverRent(leaseMediaX, leaseMediaX.getStartDate(), null, "7");
 
         Lease leasePoison = leases.findLeaseByReference("OXF-POISON-003");
         createLeaseTermForRent(leasePoison, leasePoison.getStartDate(), null, BigDecimal.valueOf(87300), null, null, null, "ISTAT-FOI");
         createLeaseTermForRent(leasePoison, leasePoison.getStartDate().plusYears(1), null, BigDecimal.valueOf(87300), new LocalDate(2011, 1, 1), new LocalDate(2012, 1, 1), new LocalDate(2012, 4, 1), "ISTAT-FOI");
         createLeaseTermForServiceCharge(leasePoison, leasePoison.getStartDate(), null, BigDecimal.valueOf(12400));
         createLeaseTermForTurnoverRent(leasePoison, leasePoison.getStartDate(), null, "7");
 
         Lease leaseM = leases.findLeaseByReference("OXF-MIRACL-005");
         createLeaseTermForRent(leaseM, leaseM.getStartDate(), null, BigDecimal.valueOf(150000), null, null, null, "ISTAT-FOI");
         createLeaseTermForRent(leaseM, new LocalDate(2015, 1, 1), null, null, new LocalDate(2013, 11, 1), new LocalDate(2014, 12, 1), null, "ISTAT-FOI");
         createLeaseTermForServiceCharge(leaseM, leaseM.getStartDate(), null, BigDecimal.valueOf(12400));
         createLeaseTermForServiceCharge(leaseM, new LocalDate(2014, 1, 1), null, BigDecimal.valueOf(13000));
         createLeaseTermForTurnoverRent(leaseM, leaseM.getStartDate(), null, "7");
 
         Lease leaseP = leases.findLeaseByReference("KAL-POISON-001");
         createLeaseTermForRent(leaseP, leaseP.getStartDate(), null, BigDecimal.valueOf(150000), null, null, null, "ISTAT-FOI");
         
     }
 
     private LeaseTerm createLeaseTermForRent(
             final Lease lease,
             final LocalDate startDate,
             final LocalDate endDate,
             final BigDecimal baseValue,
             final LocalDate baseIndexStartDate,
             final LocalDate nextIndexStartDate,
             final LocalDate effectiveDate,
             final String indexReference) {
 
         LeaseItem leaseItem = findOrCreateLeaseItem(lease, "RENT", LeaseItemType.RENT, InvoicingFrequency.QUARTERLY_IN_ADVANCE);
 
         final Index index = indices.findIndex(indexReference);
         LeaseTermForIndexableRent leaseTerm = (LeaseTermForIndexableRent) leaseItem.newTerm(startDate);
         leaseTerm.setEndDate(endDate);
         leaseTerm.setBaseValue(baseValue);
         leaseTerm.setBaseIndexStartDate(baseIndexStartDate);
         leaseTerm.setNextIndexStartDate(nextIndexStartDate);
         leaseTerm.setEffectiveDate(effectiveDate);
         leaseTerm.setFrequency(LeaseTermFrequency.YEARLY);
         leaseTerm.setIndex(index);
         return leaseTerm;
     }
 
     // //////////////////////////////////////
 
     public LeaseTerm createLeaseTermForServiceCharge(
             final Lease lease,
             final LocalDate startDate,
             final LocalDate endDate,
             final BigDecimal budgetedValue) {
         LeaseItem leaseItem = findOrCreateLeaseItem(lease, "SERVICE_CHARGE", LeaseItemType.SERVICE_CHARGE, InvoicingFrequency.QUARTERLY_IN_ADVANCE);
         LeaseTermForServiceCharge leaseTerm = (LeaseTermForServiceCharge) leaseItem.newTerm(startDate);
         leaseTerm.modifyEndDate(endDate);
         leaseTerm.setBudgetedValue(budgetedValue);
         leaseTerm.setFrequency(LeaseTermFrequency.YEARLY);
         return leaseTerm;
     }
 
     // //////////////////////////////////////
 
     public LeaseTerm createLeaseTermForTurnoverRent(
             final Lease lease,
             final LocalDate startDate,
             final LocalDate endDate,
             final String turnoverRentRule) {
         LeaseItem leaseItem = findOrCreateLeaseItem(lease, "TURNOVER_RENT", LeaseItemType.TURNOVER_RENT, InvoicingFrequency.YEARLY_IN_ARREARS);
         LeaseTermForTurnoverRent leaseTerm = (LeaseTermForTurnoverRent) leaseTerms.newLeaseTerm(leaseItem, null, startDate);
         leaseTerm.initialize();
         leaseTerm.setEndDate(endDate);
         leaseTerm.setFrequency(LeaseTermFrequency.YEARLY);
         leaseTerm.setTurnoverRentRule(turnoverRentRule);
         return leaseTerm;
     }
 
     // //////////////////////////////////////
 
     private LeaseItem findOrCreateLeaseItem(
             final Lease lease,
             final String chargeReference,
             final LeaseItemType leaseItemType,
             final InvoicingFrequency invoicingFrequency) {
         final Charge charge = charges.findCharge(chargeReference);
         return findOrCreateLeaseItem(lease, charge, leaseItemType, invoicingFrequency);
     }
 
     public LeaseItem findOrCreateLeaseItem(
             final Lease lease,
             final Charge charge,
             final LeaseItemType leaseItemType,
             final InvoicingFrequency invoicingFrequency) {
         LeaseItem li = lease.findItem(leaseItemType, lease.getStartDate(), BigInteger.ONE);
         if (li == null) {
             li = lease.newItem(leaseItemType, charge, invoicingFrequency, PaymentMethod.DIRECT_DEBIT, lease.getStartDate());
             li.setType(leaseItemType);
            li.setStatus(LeaseItemStatus.ACTIVE);
             li.setEndDate(lease.getEndDate());
             li.setSequence(BigInteger.valueOf(1));
         }
         return li;
     }
 
     // //////////////////////////////////////
 
     private Indices indices;
 
     public void injectIndices(final Indices indices) {
         this.indices = indices;
     }
 
     private Leases leases;
 
     public void injectLeases(final Leases leases) {
         this.leases = leases;
     }
 
     private LeaseTerms leaseTerms;
 
     public void injectLeaseTerms(final LeaseTerms leaseTerms) {
         this.leaseTerms = leaseTerms;
     }
 
     private Charges charges;
 
     public void injectCharges(final Charges charges) {
         this.charges = charges;
     }
 
 }
