 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package com.dreamoval.motech.imp.util;
 
 import com.dreamoval.motech.core.model.IncMessageFormParameterStatus;
 import com.dreamoval.motech.core.model.IncomingMessageFormParameter;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.regex.Pattern;
 import org.apache.log4j.Logger;
 
 /**
  *
  * @author user
  */
 public class ParamExpressionValidator implements IncomingMessageFormParameterValidator {
 
     private String expression;
     private String dateFormat;
     private static Logger logger = Logger.getLogger(ParamExpressionValidator.class);
 
     public boolean validate(IncomingMessageFormParameter param) {
         String paramType = param.getIncomingMsgFormParamDefinition().getParamType().toUpperCase();
 
         if (!Pattern.matches(expression, param.getValue().trim())) {
             param.setErrCode(1);
             param.setErrText("wrong format");
             param.setMessageFormParamStatus(IncMessageFormParameterStatus.INVALID);
         } else {
             param.setMessageFormParamStatus(IncMessageFormParameterStatus.VALID);
             if (paramType.indexOf("DATE") >= 0) {
                 try {
                     String dateInputFormat = "";
                     if (Pattern.matches("(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[012])/(19|20)?\\d\\d", param.getValue())) {
                        dateInputFormat = "dd/MM/yy";
                     } else if (Pattern.matches("\\d+", param.getValue())) {
                        dateInputFormat = "ddMMyy";
                     } else if (Pattern.matches("(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-(19|20)?\\d\\d", param.getValue())) {
                        dateInputFormat = "dd-MM-yy";
                     } else if (Pattern.matches("(0[1-9]|[12][0-9]|3[01]).(0[1-9]|1[012]).(19|20)?\\d\\d", param.getValue())) {
                        dateInputFormat = "dd.MM.yy";
                     } else if (Pattern.matches("(0[1-9]|[12][0-9]|3[01]) (0[1-9]|1[012]) (19|20)?\\d\\d", param.getValue())) {
                        dateInputFormat = "dd MM yy";
                     }
 
                     SimpleDateFormat dFormat = new SimpleDateFormat(dateInputFormat);
                     dFormat.setLenient(true);
                     Date val = dFormat.parse(param.getValue());
 
                     if (paramType.equalsIgnoreCase("DATE") && val.after(new Date())) {
                         param.setErrCode(1);
                         param.setErrText("invalid date");
                         param.setMessageFormParamStatus(IncMessageFormParameterStatus.INVALID);
                     }
                     
                     dFormat.applyPattern(dateFormat);
                     param.setValue(dFormat.format(val));
                 } catch (ParseException ex) {
                     logger.error("Invalid date format - " + param.getValue(), ex);
 
                     param.setErrCode(1);
                     param.setErrText("wrong format");
                     param.setMessageFormParamStatus(IncMessageFormParameterStatus.INVALID);
                 }
             }
         }
 
         param.setLastModified(new Date());
         return param.getMessageFormParamStatus().equals(IncMessageFormParameterStatus.VALID);
     }
 
     /**
      * @param expression the expression to set
      */
     public void setExpression(String expression) {
         this.expression = expression;
     }
 
     /**
      * @param dateFormat the dateFormat to set
      */
     public void setDateFormat(String dateFormat) {
         this.dateFormat = dateFormat;
     }
 }
