 /**
  * ***************************************************************************
  * Copyright (c) 2010 Qcadoo Limited
  * Project: Qcadoo Framework
  * Version: 1.1.5
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
 package com.qcadoo.model.internal;
 
 import static com.google.common.base.Preconditions.checkArgument;
 
 import java.math.BigDecimal;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.apache.commons.lang.builder.EqualsBuilder;
 import org.apache.commons.lang.builder.HashCodeBuilder;
 
 import com.google.common.collect.Lists;
 import com.qcadoo.model.api.DataDefinition;
 import com.qcadoo.model.api.Entity;
 import com.qcadoo.model.api.EntityList;
 import com.qcadoo.model.api.EntityTree;
 import com.qcadoo.model.api.FieldDefinition;
 import com.qcadoo.model.api.types.BelongsToType;
 import com.qcadoo.model.api.validators.ErrorMessage;
 
 public final class DefaultEntity implements Entity {
 
     private Long id;
 
     private final DataDefinition dataDefinition;
 
     private final Map<String, Object> fields;
 
     private final List<ErrorMessage> globalErrors = new ArrayList<ErrorMessage>();
 
     private final Map<String, ErrorMessage> errors = new HashMap<String, ErrorMessage>();
 
     private boolean notValidFlag = false;
 
     private boolean active = true;
 
     public DefaultEntity(final DataDefinition dataDefinition, final Long id, final Map<String, Object> fields) {
         this.dataDefinition = dataDefinition;
         this.id = id;
         this.fields = fields;
     }
 
     public DefaultEntity(final DataDefinition dataDefinition, final Long id) {
         this(dataDefinition, id, new HashMap<String, Object>());
     }
 
     public DefaultEntity(final DataDefinition dataDefinition) {
         this(dataDefinition, null, new HashMap<String, Object>());
     }
 
     @Override
     public void setId(final Long id) {
         this.id = id;
     }
 
     @Override
     public Long getId() {
         return id;
     }
 
     @Override
     public void setActive(final boolean active) {
         this.active = active;
     }
 
     @Override
     public void setField(final String fieldName, final Object fieldValue) {
         fields.put(fieldName, fieldValue);
     }
 
     @Override
     public Map<String, Object> getFields() {
         return fields;
     }
 
     @Override
     public void addGlobalError(final String message, final String... vars) {
         globalErrors.add(new ErrorMessage(message, vars));
     }
 
     @Override
     public void addError(final FieldDefinition fieldDefinition, final String message, final String... vars) {
         errors.put(fieldDefinition.getName(), new ErrorMessage(message, vars));
     }
 
     @Override
     public List<ErrorMessage> getGlobalErrors() {
         return globalErrors;
     }
 
     @Override
     public Map<String, ErrorMessage> getErrors() {
         return errors;
     }
 
     @Override
     public ErrorMessage getError(final String fieldName) {
         return errors.get(fieldName);
     }
 
     @Override
     public boolean isValid() {
         return !notValidFlag && errors.isEmpty() && globalErrors.isEmpty();
     }
 
     @Override
     public boolean isFieldValid(final String fieldName) {
         return errors.get(fieldName) == null;
     }
 
     @Override
     public void setNotValid() {
         notValidFlag = true;
     }
 
     @Override
     public int hashCode() {
         HashCodeBuilder hcb = new HashCodeBuilder(23, 41).append(id).append(dataDefinition);
 
         for (Map.Entry<String, Object> field : fields.entrySet()) {
             if (field.getValue() instanceof Collection) {
                 continue;
             }
             if (field.getValue() instanceof Entity) {
                 Entity entity = (Entity) field.getValue();
                 hcb.append(field.getKey()).append(entity.getDataDefinition().getPluginIdentifier())
                         .append(entity.getDataDefinition().getName()).append(entity.getId());
             } else {
                 hcb.append(field.getKey()).append(field.getValue());
             }
         }
 
         return hcb.toHashCode();
     }
 
     @Override
     public boolean equals(final Object obj) {
         if (obj == null) {
             return false;
         }
         if (obj == this) {
             return true;
         }
         if (!(obj instanceof DefaultEntity)) {
             return false;
         }
         DefaultEntity other = (DefaultEntity) obj;
         EqualsBuilder eb = new EqualsBuilder().append(id, other.id).append(dataDefinition, other.dataDefinition);
 
         for (Map.Entry<String, Object> field : fields.entrySet()) {
             if (field.getValue() instanceof Collection) {
                 continue;
             }
             eb.append(field.getValue(), other.fields.get(field.getKey()));
         }
 
         return eb.isEquals();
     }
 
     @Override
     public DefaultEntity copy() {
         DefaultEntity entity = new DefaultEntity(dataDefinition, id);
         for (Map.Entry<String, Object> field : fields.entrySet()) {
             if (field.getValue() instanceof Entity) {
                 entity.setField(field.getKey(), ((Entity) field.getValue()).copy());
             } else {
                 entity.setField(field.getKey(), field.getValue());
             }
         }
         return entity;
     }
 
     @Override
     public Object getField(final String fieldName) {
         return fields.get(fieldName);
     }
 
     @Override
     public String getStringField(final String fieldName) {
         return (String) getField(fieldName);
     }
 
     @Override
     public boolean getBooleanField(final String fieldName) {
         Object fieldValue = getField(fieldName);
         if (fieldValue instanceof Boolean) {
             return ((Boolean) fieldValue).booleanValue();
         }
         if (fieldValue instanceof String) {
             return Boolean.parseBoolean((String) fieldValue);
         }
         return false;
     }
 
     @Override
     public BigDecimal getDecimalField(final String fieldName) {
         Object fieldValue = getField(fieldName);
         if (fieldValue == null) {
             return null;
         }
         if (fieldValue instanceof BigDecimal) {
             return (BigDecimal) fieldValue;
         }
         throw new IllegalArgumentException("Field " + fieldName + " in " + dataDefinition.getPluginIdentifier() + '.'
                 + dataDefinition.getName() + " does not contain BigDecimal value");
     }
 
     @SuppressWarnings("unchecked")
     @Override
     public EntityList getHasManyField(final String fieldName) {
         Object fieldValue = getField(fieldName);
         if (fieldValue == null) {
             return new DetachedEntityListImpl(dataDefinition, null);
         }
         if (fieldValue instanceof EntityList) {
             return (EntityList) fieldValue;
         }
         if (fieldValue instanceof List<?>) {
             return new DetachedEntityListImpl(dataDefinition, (List<Entity>) fieldValue);
         }
         throw new IllegalArgumentException("Field " + fieldName + " in " + dataDefinition.getPluginIdentifier() + '.'
                 + dataDefinition.getName() + " does not contain value of type List<Entity> or EntityList");
     }
 
     @SuppressWarnings("unchecked")
     @Override
     public List<Entity> getManyToManyField(final String fieldName) {
         if (getField(fieldName) == null) {
             return Lists.newArrayList();
         }
         return Lists.newArrayList((Set<Entity>) getField(fieldName));
     }
 
     @Override
     public EntityTree getTreeField(final String fieldName) {
         return (EntityTree) getField(fieldName);
     }
 
     @Override
     public Entity getBelongsToField(final String fieldName) {
         if (getField(fieldName) == null) {
             return null;
         }
 
         checkArgument(dataDefinition.getField(fieldName).getType() instanceof BelongsToType, "Field should be belongsTo type");
        if (getField(fieldName) instanceof Long) {
             return getProxyForBelongsToField(fieldName);
         }
         return (Entity) getField(fieldName);
     }
 
     private Entity getProxyForBelongsToField(final String fieldName) {
         BelongsToType belongsToType = (BelongsToType) dataDefinition.getField(fieldName).getType();
        Long belongsToEntityId = (Long) getField(fieldName);
         return new ProxyEntity(belongsToType.getDataDefinition(), belongsToEntityId);
     }
 
     @Override
     public boolean isActive() {
         return active;
     }
 
     @Override
     public DataDefinition getDataDefinition() {
         return dataDefinition;
     }
 
     @Override
     public String toString() {
         StringBuilder entity = new StringBuilder("Entity[" + dataDefinition + "][id=" + id + ",active=" + active);
         for (Map.Entry<String, Object> field : fields.entrySet()) {
 
             entity.append(",").append(field.getKey()).append("=");
             if (field.getValue() instanceof Collection) {
                 entity.append("#collection");
                 continue;
             }
 
             if (field.getValue() instanceof Entity) {
                 Entity belongsToEntity = (Entity) field.getValue();
                 entity.append("Entity[" + belongsToEntity.getDataDefinition() + "][id=" + belongsToEntity.getId() + "]");
             } else {
                 entity.append(field.getValue());
             }
         }
         return entity.append("]").toString();
     }
 }
