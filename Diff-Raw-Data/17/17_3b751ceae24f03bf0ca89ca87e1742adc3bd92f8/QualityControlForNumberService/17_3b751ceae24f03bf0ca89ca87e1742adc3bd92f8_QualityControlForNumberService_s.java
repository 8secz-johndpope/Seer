 /**
  * ***************************************************************************
  * Copyright (c) 2010 Qcadoo Limited
  * Project: Qcadoo MES
  * Version: 0.4.8
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
 package com.qcadoo.mes.qualityControls;
 
 import java.util.List;
 
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Service;
 
 import com.qcadoo.model.api.DataDefinition;
 import com.qcadoo.model.api.DataDefinitionService;
 import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchQueryBuilder;
 import com.qcadoo.model.api.search.SearchResult;
 
 @Service
 public class QualityControlForNumberService {
 
     @Autowired
     private DataDefinitionService dataDefinitionService;
 
     public void checkUniqueNumber(final DataDefinition dataDefinition, final Entity entity) {
 
         String qualityControlType = entity.getStringField("qualityControlType");
         String number = entity.getStringField("number");
 
        SearchQueryBuilder searchQueryBuilder = dataDefinition.find("where number = '" + number + "' and qualityControlType = '"
                + qualityControlType + "'");
 
        if (searchQueryBuilder != null) {
            SearchResult searchResult = searchQueryBuilder.list();
             if (searchResult != null && searchResult.getTotalNumberOfEntities() > 0) {
                 entity.addError(dataDefinition.getField("number"), "qualityControls.quality.control.validate.global.error.number");
             }
         }
     }
 
     public String generateNumber(final String plugin, final String model, final int digitsNumber, final String qualityControlType) {
         long longValue = 0;
 
         SearchResult searchResult = dataDefinitionService.get(plugin, model)
                 .find("where qualityControlType = '" + qualityControlType + "'").setMaxResults(1).list();
         if (searchResult == null || searchResult.getEntities().isEmpty()) {
             longValue++;
         } else {
             List<Entity> entityList = searchResult.getEntities();
             Entity entity = entityList.get(0);
             longValue = entity.getId() + 1;
         }
         return String.format("%0" + digitsNumber + "d", longValue);
     }
 }
