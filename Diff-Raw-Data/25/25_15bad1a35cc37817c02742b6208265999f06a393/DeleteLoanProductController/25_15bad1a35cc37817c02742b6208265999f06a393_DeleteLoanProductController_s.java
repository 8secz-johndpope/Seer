 /*
  * Copyright (c) 2005-2008 Grameen Foundation USA
  * All rights reserved.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *     http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
  * implied. See the License for the specific language governing
  * permissions and limitations under the License.
  * 
  * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
  * explanation of the license and how it is applied.
  */
 
 package org.mifos.ui.loan.controller;
 
 import java.util.HashMap;
 import java.util.Map;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.mifos.loan.service.LoanProductDto;
 import org.mifos.loan.service.LoanProductService;
 import org.springframework.validation.BindException;
 import org.springframework.web.servlet.ModelAndView;
 import org.springframework.web.servlet.mvc.SimpleFormController;
 
 public class DeleteLoanProductController extends SimpleFormController {
 
     private LoanProductService loanProductService;
     
 
        @Override
         protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors)  {
                Map<String, Object> model = errors.getModel();
//                model.put("deleteLoanProduct", new DeleteLoanProductDto());
                return new ModelAndView("deleteLoanProduct", model);
         }
         
        @Override
        @SuppressWarnings("PMD.SignatureDeclareThrowsException") //rationale: This is the signature of the superclass's method that we're overriding
        protected Map referenceData(HttpServletRequest request) throws Exception {      
        Map<String, Object> referenceData = new HashMap<String, Object>();
        referenceData.put("client", new DeleteLoanProductDto());
        return referenceData;
        }
        
         @Override
         @SuppressWarnings("PMD.SignatureDeclareThrowsException") //rationale: This is the signature of the superclass's method that we're overriding
         protected ModelAndView onSubmit(Object command) throws Exception {
             LoanProductDto loanProductDto = loanProductService.getLoanProduct(((DeleteLoanProductDto) command).getLoanProductId());
             loanProductService.deleteLoanProduct(loanProductDto);
             Map<String, Object> model = new HashMap<String, Object>();
            return new ModelAndView("createClientSuccess", "model", model);
         }
 
         @SuppressWarnings("PMD.SignatureDeclareThrowsException") //rationale: This is the signature of the superclass's method that we're overriding
         protected Object formBackingObject(HttpServletRequest request) throws Exception {
             return new DeleteLoanProductDto();
         }
         
     public void setLoanProductService(LoanProductService loanProductService) {
         this.loanProductService = loanProductService;
     }
 
     }
