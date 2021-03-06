 /**
  * ***************************************************************************
  * Copyright (c) 2010 Qcadoo Limited
  * Project: Qcadoo MES
  * Version: 0.2.0
  *
  * This file is part of Qcadoo.
  *
  * Qcadoo is free software; you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published
  * by the Free Software Foundation; either version 3 of the License,
  * or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty
  * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  * ***************************************************************************
  */
 
 package com.qcadoo.mes.model.types.internal;
 
 import org.springframework.security.authentication.encoding.PasswordEncoder;
 
 import com.qcadoo.mes.api.Entity;
 import com.qcadoo.mes.model.FieldDefinition;
 import com.qcadoo.mes.model.types.FieldType;
 
 public final class PasswordType implements FieldType {
 
     private final PasswordEncoder passwordEncoder;
 
     public PasswordType(final PasswordEncoder passwordEncoder) {
         this.passwordEncoder = passwordEncoder;
     }
 
     @Override
     public boolean isSearchable() {
         return false;
     }
 
     @Override
     public boolean isOrderable() {
         return false;
     }
 
     @Override
     public boolean isAggregable() {
         return false;
     }
 
     @Override
     public Class<?> getType() {
         return String.class;
     }
 
     @Override
     public Object toObject(final FieldDefinition fieldDefinition, final Object value, final Entity validatedEntity) {
        System.out.println(" @ ---> " + value);
         return passwordEncoder.encodePassword(String.valueOf(value), null);
     }
 
     @Override
     public String toString(final Object value) {
         return null;
     }
 
 }
