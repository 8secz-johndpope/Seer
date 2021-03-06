 /*
 // $Id$
 // Farrago is an extensible data management system.
 // Copyright (C) 2005-2005 The Eigenbase Project
 // Copyright (C) 2005-2005 Disruptive Tech
 // Copyright (C) 2005-2005 LucidEra, Inc.
 //
 // This program is free software; you can redistribute it and/or modify it
 // under the terms of the GNU General Public License as published by the Free
 // Software Foundation; either version 2 of the License, or (at your option)
 // any later version approved by The Eigenbase Project.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 package net.sf.farrago.defimpl;
 
 import com.disruptivetech.farrago.calc.*;
 import com.disruptivetech.farrago.fennel.*;
 
 import com.lucidera.farrago.fennel.*;
 import com.lucidera.lurql.*;
 
 import java.io.*;
 import java.util.*;
 
 import javax.jmi.reflect.*;
 
 import net.sf.farrago.catalog.*;
 import net.sf.farrago.cwm.core.*;
 import net.sf.farrago.cwm.relational.*;
 import net.sf.farrago.cwm.relational.enumerations.*;
 import net.sf.farrago.db.*;
 import net.sf.farrago.ddl.*;
 import net.sf.farrago.fem.security.*;
 import net.sf.farrago.parser.*;
 import net.sf.farrago.query.*;
 import net.sf.farrago.resource.*;
 import net.sf.farrago.runtime.*;
 import net.sf.farrago.session.*;
 
 import org.eigenbase.jmi.*;
 import org.eigenbase.oj.rex.*;
 import org.eigenbase.rel.metadata.*;
 import org.eigenbase.resgen.*;
 import org.eigenbase.resource.*;
 import org.eigenbase.sql.*;
 import org.eigenbase.sql.fun.*;
 import org.eigenbase.sql.type.*;
 import org.eigenbase.util.*;
 import org.eigenbase.util14.*;
 
 
 /**
  * FarragoDefaultSessionPersonality is a default implementation of the {@link
  * FarragoSessionPersonality} interface.
  *
  * @author John V. Sichi
  * @version $Id$
  */
 public class FarragoDefaultSessionPersonality
     implements FarragoSessionPersonality
 {
 
     //~ Static fields ----------------------------------------------------------
 
     public static final String SQUEEZE_JDBC_NUMERIC = "squeezeJdbcNumeric";
     public static final String SQUEEZE_JDBC_NUMERIC_DEFAULT = "true";
 
     //~ Instance fields --------------------------------------------------------
 
     protected final FarragoDatabase database;
     protected final ParamValidator paramValidator;
 
     //~ Constructors -----------------------------------------------------------
 
     protected FarragoDefaultSessionPersonality(FarragoDbSession session)
     {
         database = session.getDatabase();
 
         paramValidator = new ParamValidator();
         paramValidator.registerBoolParam(
             SQUEEZE_JDBC_NUMERIC, false);
     }
 
     //~ Methods ----------------------------------------------------------------
 
     // implement FarragoSessionPersonality
     public FarragoSessionPlanner newPlanner(
         FarragoSessionPreparingStmt stmt,
         boolean init)
     {
         return FarragoDefaultHeuristicPlanner.newInstance(stmt);
     }
 
     // implement FarragoSessionPersonality
     public void definePlannerListeners(FarragoSessionPlanner planner)
     {
     }
 
     // implement FarragoStreamFactoryProvider
     public void registerStreamFactories(long hStreamGraph)
     {
         DisruptiveTechJni.registerStreamFactory(hStreamGraph);
         LucidEraJni.registerStreamFactory(hStreamGraph);
     }
 
     // implement FarragoSessionPersonality
     public String getDefaultLocalDataServerName(
         FarragoSessionStmtValidator stmtValidator)
     {
         if (stmtValidator.getSession().getRepos().isFennelEnabled()) {
             return "SYS_FTRS_DATA_SERVER";
         } else {
             return "SYS_MOCK_DATA_SERVER";
         }
     }
 
     // implement FarragoSessionPersonality
     public SqlOperatorTable getSqlOperatorTable(
         FarragoSessionPreparingStmt preparingStmt)
     {
         return SqlStdOperatorTable.instance();
     }
 
     // implement FarragoSessionPersonality
     public OJRexImplementorTable getOJRexImplementorTable(
         FarragoSessionPreparingStmt preparingStmt)
     {
         return database.getOJRexImplementorTable();
     }
 
     // implement FarragoSessionPersonality
     public <C> C newComponentImpl(Class<C> componentInterface)
     {
         if (componentInterface == CalcRexImplementorTable.class) {
             return componentInterface.cast(CalcRexImplementorTableImpl.std());
         }
 
         return null;
     }
 
     // implement FarragoSessionPersonality
     public FarragoSessionParser newParser(FarragoSession session)
     {
         return new FarragoParser();
     }
 
     // implement FarragoSessionPersonality
     public FarragoSessionPreparingStmt newPreparingStmt(
         FarragoSessionStmtValidator stmtValidator)
     {
         return newPreparingStmt(null, stmtValidator);
     }
 
     // implement FarragoSessionPersonality
     public FarragoSessionPreparingStmt newPreparingStmt(
         FarragoSessionStmtContext stmtContext,
         FarragoSessionStmtValidator stmtValidator)
     {
         // NOTE: We don't use stmtContext here (except to obtain the SQL text),
         // and we don't pass it on to the
         // preparing statement, because that doesn't need to be aware of its
         // context. However, custom personalities may have a use for it, which
         // is why it is provided in the interface.
        String sql = stmtContext.getSql();
         FarragoPreparingStmt stmt =
             new FarragoPreparingStmt(stmtValidator, sql);
         FarragoSessionPlanner planner =
             stmtValidator.getSession().getPersonality().newPlanner(stmt, true);
         planner.setRuleDescExclusionFilter(
             stmtValidator.getSession().getOptRuleDescExclusionFilter());
         stmt.setPlanner(planner);
         return stmt;
     }
 
     // implement FarragoSessionPersonality
     public FarragoSessionDdlValidator newDdlValidator(
         FarragoSessionStmtValidator stmtValidator)
     {
         return new DdlValidator(stmtValidator);
     }
 
     // implement FarragoSessionPersonality
     public void defineDdlHandlers(
         FarragoSessionDdlValidator ddlValidator,
         List handlerList)
     {
         // NOTE jvs 21-Jan-2005:  handlerList order matters here.
         // DdlRelationalHandler includes some catch-all methods for
         // superinterfaces which we only want to invoke when one of
         // the more specific handlers doesn't satisfied the request.
         DdlMedHandler medHandler = new DdlMedHandler(ddlValidator);
         DdlSecurityHandler securityHandler =
             new DdlSecurityHandler(ddlValidator);
         handlerList.add(medHandler);
         handlerList.add(new DdlRoutineHandler(ddlValidator));
         handlerList.add(securityHandler);
         handlerList.add(new DdlRelationalHandler(medHandler));
 
         // Define drop rules
         FarragoRepos repos =
             ddlValidator.getStmtValidator().getSession().getRepos();
 
         // When a table is dropped, all indexes on the table should also be
         // implicitly dropped.
         ddlValidator.defineDropRule(
             repos.getKeysIndexesPackage().getIndexSpansClass(),
             new FarragoSessionDdlDropRule("spannedClass",
                 null,
                 ReferentialRuleTypeEnum.IMPORTED_KEY_CASCADE));
 
         // Dependencies can never be dropped without CASCADE, but with
         // CASCADE, they go away.
         ddlValidator.defineDropRule(
             repos.getCorePackage().getDependencySupplier(),
             new FarragoSessionDdlDropRule("supplier",
                 null,
                 ReferentialRuleTypeEnum.IMPORTED_KEY_RESTRICT));
 
         // When a dependency gets dropped, take its owner (the client)
         // down with it.
         ddlValidator.defineDropRule(
             repos.getCorePackage().getElementOwnership(),
             new FarragoSessionDdlDropRule("ownedElement",
                 CwmDependency.class,
                 ReferentialRuleTypeEnum.IMPORTED_KEY_CASCADE));
 
         // Without CASCADE, a schema can only be dropped when it is empty.
         // This is not true for other namespaces (e.g. a table's constraints
         // are dropped implicitly), so we specify the superInterface filter.
         ddlValidator.defineDropRule(
             repos.getCorePackage().getElementOwnership(),
             new FarragoSessionDdlDropRule("namespace",
                 CwmSchema.class,
                 ReferentialRuleTypeEnum.IMPORTED_KEY_RESTRICT));
 
         // When a UDT is dropped, all routines which realize methods should
         // also be implicitly dropped.
         ddlValidator.defineDropRule(
             repos.getBehavioralPackage().getOperationMethod(),
             new FarragoSessionDdlDropRule("specification",
                 null,
                 ReferentialRuleTypeEnum.IMPORTED_KEY_CASCADE));
 
         // Grants should be dropped together with any of the grantor, grantee,
         // or granted element
         ddlValidator.defineDropRule(
             repos.getSecurityPackage().getPrivilegeIsGrantedToGrantee(),
             new FarragoSessionDdlDropRule(
                 "Grantee",
                 null,
                 ReferentialRuleTypeEnum.IMPORTED_KEY_CASCADE));
         ddlValidator.defineDropRule(
             repos.getSecurityPackage().getPrivilegeIsGrantedByGrantor(),
             new FarragoSessionDdlDropRule(
                 "Grantor",
                 null,
                 ReferentialRuleTypeEnum.IMPORTED_KEY_CASCADE));
         ddlValidator.defineDropRule(
             repos.getSecurityPackage().getPrivilegeIsGrantedOnElement(),
             new FarragoSessionDdlDropRule(
                 "Element",
                 null,
                 ReferentialRuleTypeEnum.IMPORTED_KEY_CASCADE));
     }
 
     // implement FarragoSessionPersonality
     public void definePrivileges(
         FarragoSessionPrivilegeMap map)
     {
         FarragoRepos repos = database.getSystemRepos();
 
         PrivilegedAction [] tableActions =
             new PrivilegedAction[] {
                 PrivilegedActionEnum.SELECT,
                 PrivilegedActionEnum.INSERT,
                 PrivilegedActionEnum.DELETE,
                 PrivilegedActionEnum.UPDATE,
             };
         defineTypePrivileges(
             map,
             repos.getRelationalPackage().getCwmNamedColumnSet(),
             tableActions);
 
         PrivilegedAction [] routineActions =
             new PrivilegedAction[] { PrivilegedActionEnum.EXECUTE };
         defineTypePrivileges(
             map,
             repos.getSql2003Package().getFemRoutine(),
             routineActions);
     }
 
     private void defineTypePrivileges(
         FarragoSessionPrivilegeMap map,
         RefClass refClass,
         PrivilegedAction [] actions)
     {
         for (int i = 0; i < actions.length; ++i) {
             map.mapPrivilegeForType(
                 refClass,
                 actions[i].toString(),
                 true,
                 true);
         }
     }
 
     // implement FarragoSessionPersonality
     public Class getRuntimeContextClass(
         FarragoSessionPreparingStmt preparingStmt)
     {
         return FarragoRuntimeContext.class;
     }
 
     // implement FarragoSessionPersonality
     public FarragoSessionRuntimeContext newRuntimeContext(
         FarragoSessionRuntimeParams params)
     {
         return new FarragoRuntimeContext(params);
     }
 
     // implement FarragoSessionPersonality
     public void validate(
         FarragoSessionStmtValidator stmtValidator,
         SqlNode sqlNode)
     {
     }
 
     // implement FarragoSessionPersonality
     public void loadDefaultSessionVariables(
         FarragoSessionVariables variables)
     {
         variables.setDefault(
             SQUEEZE_JDBC_NUMERIC,
             SQUEEZE_JDBC_NUMERIC_DEFAULT);
     }
 
     // implement FarragoSessionPersonality
     public void validateSessionVariable(
         FarragoSessionDdlValidator ddlValidator,
         FarragoSessionVariables variables,
         String name,
         String value)
     {
         String validatedValue = 
             paramValidator.validate(ddlValidator, name, value);
         variables.set(name, validatedValue);
     }
 
     // implement FarragoSessionPersonality
     public JmiQueryProcessor newJmiQueryProcessor(String language)
     {
         if (!language.equals("LURQL")) {
             return null;
         }
         return new LurqlQueryProcessor(
                 database.getSystemRepos().getMdrRepos());
     }
 
     public boolean isSupportedType(SqlTypeName type)
     {
         if (type == null) {
             // Not a SQL type -- may be a structured type, such as MULTISET.
             return true;
         }
         switch (type.getOrdinal()) {
         case SqlTypeName.Boolean_ordinal:
         case SqlTypeName.Tinyint_ordinal:
         case SqlTypeName.Smallint_ordinal:
         case SqlTypeName.Integer_ordinal:
         case SqlTypeName.Date_ordinal:
         case SqlTypeName.Time_ordinal:
         case SqlTypeName.Timestamp_ordinal:
         case SqlTypeName.Bigint_ordinal:
         case SqlTypeName.Varchar_ordinal:
         case SqlTypeName.Varbinary_ordinal:
         case SqlTypeName.Multiset_ordinal:
         case SqlTypeName.Char_ordinal:
         case SqlTypeName.Binary_ordinal:
         case SqlTypeName.Real_ordinal:
         case SqlTypeName.Float_ordinal:
         case SqlTypeName.Double_ordinal:
         case SqlTypeName.Row_ordinal:
         case SqlTypeName.Decimal_ordinal:
             return true;
         case SqlTypeName.Distinct_ordinal:
         default:
             return false;
         }
     }
 
     // implement FarragoSessionPersonality
     public boolean supportsFeature(ResourceDefinition feature)
     {
         // TODO jvs 20-Mar-2006: Fix this non-conforming behavior.  According
         // to the JDBC spec, each statement in an autocommit connection is
         // supposed to execute in its own private transaction.  Farrago's
         // support for this isn't done yet, so for now we prevent
         // multiple active statements on an autocommit connection
         // (unless a personality specifically enables it).
         ResourceDefinition maasFeature =
             EigenbaseResource.instance()
             .SQLConformance_MultipleActiveAutocommitStatements;
         if (feature == maasFeature) {
             return false;
         }
         
         // Farrago doesn't support MERGE
         if (feature == EigenbaseResource.instance().SQLFeature_F312) {
             return false;
         }
 
         // By default, support everything except the above.
         return true;
     }
 
     // implement FarragoSessionPersonality
     public void registerRelMetadataProviders(ChainedRelMetadataProvider chain)
     {
         chain.addProvider(
             new FarragoRelMetadataProvider(database.getSystemRepos()));
     }
 
     private class ParamDesc
     {
         int type;
         boolean nullability;
         Integer rangeStart, rangeEnd;
         
         public ParamDesc(int type, boolean nullability) {
             this.type = type;
             this.nullability = nullability;
         }
 
         public ParamDesc(int type, boolean nullability, int start, int end) {
             this.type = type;
             this.nullability = nullability;
             rangeStart = start;
             rangeEnd = end;
         }
     }
     
     public class ParamValidator 
     {
         private final int BOOLEAN_TYPE = 1;
         private final int INT_TYPE = 2;
         private final int STRING_TYPE = 3;
         private final int DIRECTORY_TYPE = 4;
         
         private Map<String, ParamDesc> params;
 
         public ParamValidator()
         {
             params = new HashMap<String, ParamDesc>();
         }
 
         public void registerBoolParam(String name, boolean nullability)
         {
             params.put(name, new ParamDesc(BOOLEAN_TYPE, nullability));
         }
 
         public void registerIntParam(String name, boolean nullability)
         {
             params.put(name, new ParamDesc(INT_TYPE, nullability));
         }
 
         public void registerIntParam(
             String name, boolean nullability, int start, int end)
         {
             assert (start <= end);
             params.put(name, new ParamDesc(INT_TYPE, nullability, start, end));
         }
 
         public void registerStringParam(String name, boolean nullability)
         {
             params.put(name, new ParamDesc(STRING_TYPE, nullability));
         }
 
         public void registerDirectoryParam(String name, boolean nullability)
         {
             params.put(name, new ParamDesc(DIRECTORY_TYPE, nullability));
         }
 
         public String validate(
             FarragoSessionDdlValidator ddlValidator,
             String name, 
             String value)
         {
             if (! params.containsKey(name)) {
                 throw FarragoResource.instance().ValidatorUnknownSysParam.ex(
                     ddlValidator.getRepos().getLocalizedObjectName(name));
             }
             ParamDesc paramDesc = params.get(name);
             if (paramDesc.nullability == false && value == null) {
                 throw FarragoResource.instance()
                 .ValidatorSysParamTypeMismatch.ex(
                     value,
                     ddlValidator.getRepos().getLocalizedObjectName(name));
             } else if (value == null) {
                 return null;
             }
 
             Object o = null;
             switch (paramDesc.type) {
             case BOOLEAN_TYPE:
                 o = ConversionUtil.toBoolean(value);
                 break;
             case INT_TYPE:
                 o = Integer.valueOf(value);
                 if (paramDesc.rangeStart != null) {
                     Integer i = (Integer) o;
                     if (i < paramDesc.rangeStart || i > paramDesc.rangeEnd) {
                         throw FarragoResource.instance()
                         .ParameterValueOutOfRange.ex(value, name);
                     }
                 }
                 break;
             case STRING_TYPE:
                 o = value;
                 break;
             case DIRECTORY_TYPE:
                 File dir = new File(value);
                 if ( (!dir.exists()) || (!dir.isDirectory()) ) {
                     throw FarragoResource.instance().InvalidDirectory.ex(
                         value);
                 }
                 if (!dir.canWrite()) {
                     throw FarragoResource.instance().FileWriteFailed.ex(value);
                 }
                 o = dir.getPath();
                 break;
             default:
                 Util.permAssert(false, "invalid param type");
             }
             return o.toString();
         }
     }
 }
 
 // End FarragoDefaultSessionPersonality.java
