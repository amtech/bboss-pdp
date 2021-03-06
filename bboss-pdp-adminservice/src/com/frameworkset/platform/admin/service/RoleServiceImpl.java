/**
 *  Copyright 2008-2010 biaoping.yin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.frameworkset.platform.admin.service;

import com.frameworkset.common.poolman.ConfigSQLExecutor;
import com.frameworkset.common.poolman.Record;
import com.frameworkset.common.poolman.handle.NullRowHandler;
import com.frameworkset.common.poolman.handle.RowHandler;
import com.frameworkset.orm.transaction.TransactionManager;
import com.frameworkset.platform.admin.entity.*;
import com.frameworkset.util.ListInfo;
import org.frameworkset.platform.config.model.ResourceInfo;
import org.frameworkset.platform.resource.ResourceManager;
import org.frameworkset.platform.security.authorization.AuthRole;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

/**
 * <p>Title: RoleServiceImpl</p> <p>Description: 角色管理管理业务处理类 </p> <p>bboss</p>
 * <p>Copyright (c) 2007</p> @Date 2016-12-15 17:06:09 @author yinbp @version
 * v1.0
 */
public class RoleServiceImpl implements RoleService {

	private static org.slf4j.Logger log = LoggerFactory.getLogger(com.frameworkset.platform.admin.service.RoleServiceImpl.class);

	private ConfigSQLExecutor executor;
	private ResourceManager resourceManager = new ResourceManager();
	public boolean checkroleexist(String roleName) throws RoleException {
		// 业务组件
		try {
			int num = executor.queryObject(int.class,"checkroleexist", roleName);
			return num > 0;
		} catch (Throwable e) {
			throw new RoleException("check rolerexist failed:", e);
		}
	}
	public void addRole(Role role) throws RoleException {
		// 业务组件
		try {
			executor.insertBean("addRole", role);
		} catch (Throwable e) {
			throw new RoleException("add Role failed:", e);
		}

	}
	public void deleteRole(String roleId) throws RoleException {
		try {
			executor.delete("deleteByKey", roleId);
		} catch (Throwable e) {
			throw new RoleException("delete Role failed::roleId=" + roleId, e);
		}

	}
	public void deleteBatchRole(String... roleIds) throws RoleException {
		TransactionManager tm = new TransactionManager();
		try {
			tm.begin();
			executor.deleteByKeys("deleteByKey", roleIds);
			tm.commit();
		} catch (Throwable e) {

			throw new RoleException("batch delete Role failed::roleIds=" + roleIds, e);
		} finally {
			tm.release();
		}

	}
	public void updateRole(Role role) throws RoleException {
		try {
			executor.updateBean("updateRole", role);
		} catch (Throwable e) {
			throw new RoleException("update Role failed::", e);
		}

	}
	public Role getRole(String roleId) throws RoleException {
		try {
			Role bean = executor.queryObject(Role.class, "selectById", roleId);
			return bean;
		} catch (Throwable e) {
			throw new RoleException("get Role failed::roleId=" + roleId, e);
		}

	}
	public Role getRoleByName(String roleName) throws RoleException {
		try {
			Role bean = executor.queryObject(Role.class, "getRoleByName", roleName);
			return bean;
		} catch (Throwable e) {
			throw new RoleException("get Role failed::roleName=" + roleName, e);
		}

	}
	public ListInfo queryListInfoRoles(RoleCondition conditions, long offset, int pagesize) throws RoleException {
		ListInfo datas = null;
		try {
			//如果是在角色设置界面，则需要过滤没有授予权限的角色和guest角色
			datas = executor.queryListInfoBean(Role.class, "queryListRole", offset, pagesize, conditions);
		} catch (Exception e) {
			throw new RoleException("pagine query Role failed:", e);
		}
		return datas;

	}
	public List<Role> queryListRoles(RoleCondition conditions) throws RoleException {
		try {
			List<Role> beans = executor.queryListBean(Role.class, "queryListRole", conditions);
			return beans;
		} catch (Exception e) {
			throw new RoleException("query Role failed:", e);
		}

	}
	/** (non-Javadoc)
	 * @see com.frameworkset.platform.admin.service.RoleService#getUserRoles(java.lang.String)
	 */
	@Override
	public List<UserRole> getUserRoles(String userId) throws RoleException {
		try {
			List<UserRole> beans = executor.queryList(UserRole.class, "getUserRoles", userId);
			return beans;
		} catch (Exception e) {
			throw new RoleException("query Role failed:", e);
		}
	}
	public ListInfo queryRoleUsers(String userAttr, String roleId, long offset, int pagesize)  throws RoleException {
		try {
			Map params  = new HashMap();
			params.put("userAttr", userAttr);
			params.put("roleName", roleId);
			ListInfo beans = executor.queryListInfoBean(RoleUser.class, "queryRoleUsers",offset,pagesize,params);
			return beans;
		} catch (Exception e) {
			throw new RoleException("query Role failed:", e);
		}
	}
	/** 
	 * op_id,AUTHORIZATION_STIME,AUTHORIZATION_ETIME
	 * @see com.frameworkset.platform.admin.service.RoleService#getGrantedGlobalOperations(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Map getGrantedGlobalOperations(String globalresourceid, String resourceType, String roleId,
			String roleType, String permissionTable) throws RoleException {
		
		try {
			// rop.ROLE_ID = ? and rop.TYPES =? and RESTYPE_ID = ? and RES_ID = ?
			Map params = new HashMap();
			params.put("roleId", roleId);
			params.put("resourceType", resourceType);
			params.put("globalresourceid", globalresourceid);
			params.put("roleType", roleType);
			params.put("permissionTable", permissionTable);
			final Map ps = new HashMap();
			this.executor.queryBeanByNullRowHandler(new NullRowHandler(){

				@Override
				public void handleRow(Record origine) throws Exception {
					String op_id = origine.getString("op_id");
					Date[] stime = new Date[2];
					stime[0] = origine.getDate("AUTHORIZATION_STIME");
					stime[1] = origine.getDate("AUTHORIZATION_ETIME");
					ps.put(op_id, stime);
				}
				
			},  "getGrantedGlobalOperations", params);
			return ps;
		} catch (Exception e) {
			throw new RoleException(" getGrantedGlobalOperations failed:", e);
		}
	}
	
	/**
	 * 获取资源类型下面的，授予角色的全局操作及有效时间
	 * op_id,AUTHORIZATION_STIME,AUTHORIZATION_ETIME
	 * @param opcode
	 * @param resourceType
	 * @param roleId
	 * @param roleType
	 */
	public List<AuthOPS> getGrantedOperations(String opcode, String resourceType, String roleId,
			String roleType,String permissionTable,RowHandler rowHandler,Class poclazz)throws RoleException{
		try {
			// rop.ROLE_ID = ? and rop.TYPES =? and RESTYPE_ID = ? and RES_ID = ?
			Map params = new HashMap();
			params.put("roleId", roleId);
			params.put("resourceType", resourceType);
			params.put("opcode", opcode);
			params.put("roleType", roleType);
			params.put("permissionTable", permissionTable);
			List<AuthOPS> menus = null;
			if(rowHandler == null){
				menus = this.executor.queryListBeanByRowHandler(new RowHandler<AuthOPS>(){
	
					@Override
					public void handleRow(AuthOPS menu,Record origine) throws Exception {
						String op_id = origine.getString("op_id");
						menu.setOpcode(op_id);
						menu.setResCode(origine.getString("RES_ID"));
						menu.setResName(origine.getString("RES_NAME"));
						menu.setResourceType(origine.getString("RESTYPE_ID"));					
						menu.setAUTHORIZATION_ETIME(origine.getDate("AUTHORIZATION_ETIME"));
						menu.setAUTHORIZATION_STIME(origine.getDate("AUTHORIZATION_STIME"));						
						
					}
					
				}, AuthOPS.class, "getGrantedOperations", params);
			}
			else
			{
				menus = this.executor.queryListBeanByRowHandler(rowHandler,poclazz, "getGrantedOperations", params);
			}
			return menus;
		} catch (Exception e) {
			throw new RoleException(" getGrantedOperations failed:", e);
		}
	}
	/**
	 * 获取资源类型下面的，授予角色的全局操作及有效时间
	 * op_id,AUTHORIZATION_STIME,AUTHORIZATION_ETIME
	 * @param opcode
	 * @param resourceType
	 * @param roleId
	 * @param roleType
	 */
	public ListInfo getGrantedOperations(String opcode, String resourceType, String roleId,
			String roleType,String resourceAttr,
			 long offset,
				int pagesize,String permissionTable,RowHandler rowHandler,Class poclazz)throws RoleException{
		try {
			// rop.ROLE_ID = ? and rop.TYPES =? and RESTYPE_ID = ? and RES_ID = ?
			Map params = new HashMap();
			params.put("roleId", roleId);
			params.put("resourceType", resourceType);
			params.put("opcode", opcode);
			params.put("roleType", roleType);
			if(resourceAttr != null && !resourceAttr.trim().equals(""))
				params.put("resourceAttr", "%"+resourceAttr+"%");
			params.put("permissionTable", permissionTable);
			ListInfo menus = null;
			if(rowHandler == null){
				menus = this.executor.queryListInfoBeanByRowHandler(new RowHandler<AuthOPS>(){
	
					@Override
					public void handleRow(AuthOPS menu,Record origine) throws Exception {
						String op_id = origine.getString("op_id");
						menu.setOpcode(op_id);
						menu.setResCode(origine.getString("RES_ID"));
						menu.setResName(origine.getString("RES_NAME"));
						menu.setResourceType(origine.getString("RESTYPE_ID"));					
						menu.setAUTHORIZATION_ETIME(origine.getDate("AUTHORIZATION_ETIME"));
						menu.setAUTHORIZATION_STIME(origine.getDate("AUTHORIZATION_STIME"));						
						
					}
					
				}, AuthOPS.class, "getGrantedOperations", offset,
						 pagesize,params);
			}
			else
			{
				menus = this.executor.queryListInfoBeanByRowHandler(rowHandler,poclazz, "getGrantedOperations", offset,
						 pagesize,params);
			}
			return menus;
		} catch (Exception e) {
			throw new RoleException(" getGrantedOperations failed:", e);
		}
	}
	
	
	/**
	 * 删除角色资源操作权限
	 * @param resOprs
	 * @param resourceType
	 * @param roleId
	 * @param roleType
	 * @param permissionTable
	 */
	public void deleteRoleAuthResources(List<ResOpr> resOprs, String resourceType, String roleId, String roleType,
			String permissionTable)  throws RoleException {
		TransactionManager tm = new TransactionManager();
		try
		{
			tm.begin();
			List<Map> deleteAuths = new ArrayList<Map>();
			
			for(int i = 0;  resOprs != null && i< resOprs.size(); i ++){
				ResOpr resOpr = resOprs.get(i);
				HashMap authinfos = new HashMap();
				authinfos.put("roleId", roleId);
				authinfos.put("resCode", resOpr.getResCode());
				authinfos.put("opCode", resOpr.getOp());
				authinfos.put("resourceType", resourceType);
				authinfos.put("roleType", roleType);
				authinfos.put("permissionTable", permissionTable);
				deleteAuths.add(authinfos);
			}
			this.executor.deleteBeans("deleteRoleAuthResources", deleteAuths);
			
			tm.commit();
		}
		catch(Exception e)
		{
			throw new RoleException(e);
		}
		finally
		{
			tm.release();
		}
	}
	/**
	 * 删除角色时需要删除角色对应的所有资源
	 * (non-Javadoc)
	 * @see com.frameworkset.platform.admin.service.RoleService#deleteAllRoleAuthResources(java.lang.String[], java.lang.String)
	 */
	public void deleteAllRoleAuthResources(String[] roleIds, String roleType)  throws RoleException {
		
		TransactionManager tm = new TransactionManager();
		try
		{
			tm.begin();
			List<ResourceInfo> resourceInfos = resourceManager.getResourceInfos();
			
			if(resourceInfos == null || resourceInfos.size() == 0){
					String permissionTable = resourceManager.DEFAULT_PERMISSION_TABLE;
					List<Map> deleteAuths = new ArrayList<Map>();
					for(String roleId:roleIds){
						HashMap authinfos = new HashMap();
						authinfos.put("roleId", roleId);				 
						authinfos.put("roleType", roleType);
						authinfos.put("permissionTable", permissionTable);
						deleteAuths.add(authinfos);
					}
					this.executor.deleteBeans("deleteAllRoleAuthResources", deleteAuths); 
				 
			}
			else
			{
				Map<String,Object> handled = new HashMap<String,Object>(); 				
				for(int i = 0; i < resourceInfos.size(); i ++){
					ResourceInfo resourceInfo = resourceInfos.get(i);
					String permissionTable = resourceInfo.getPermissionTable();
					if(handled.containsKey(permissionTable))
						continue;
					handled.put(permissionTable, permissionTable);
					List<Map> deleteAuths = new ArrayList<Map>();
					for(String roleId:roleIds){
						HashMap authinfos = new HashMap();
						authinfos.put("roleId", roleId);				 
						authinfos.put("roleType", roleType);
						authinfos.put("permissionTable", permissionTable);
						deleteAuths.add(authinfos);
					}
					this.executor.deleteBeans("deleteAllRoleAuthResources", deleteAuths); 
					 
				}
				handled = null;
			}	
			
			
			tm.commit();
		}
		catch(Exception e)
		{
			throw new RoleException(e);
		}
		finally
		{
			tm.release();
		}
		
		
	}
	/** (non-Javadoc)
	 * @see com.frameworkset.platform.admin.service.RoleService#saveRoleAuths(java.lang.String, java.lang.String[], java.util.List, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void saveRoleAuths(String globalresourceid, String[] globalopcode, List<ResOpr> resOprs, String resourceType,
			String roleId, String roleType,String permissionTable) throws RoleException  {
		TransactionManager tm = new TransactionManager();
		try
		{
			tm.begin();
			List<Map> deleteAuths = new ArrayList<Map>();
			Map authinfos = authinfos = new HashMap();
			authinfos.put("roleId", roleId);
			authinfos.put("resourceType", resourceType);
			authinfos.put("roleType", roleType);
			authinfos.put("resCode", globalresourceid);
			authinfos.put("permissionTable", permissionTable);
			this.executor.deleteBean("cleanroleAuths", authinfos);
//			if(StringUtil.isNotEmpty(globalresourceid)){
//				authinfos = new HashMap();
//				authinfos.put("roleId", roleId);
//				authinfos.put("resourceType", resourceType);
//				authinfos.put("roleType", roleType);
////				authinfos.put("resCode", globalresourceid);
//				authinfos.put("permissionTable", permissionTable);
//				deleteAuths.add(authinfos);
//			}
//				
//			
//			for(int i = 0;  resOprs != null && i< resOprs.size(); i ++){
//				ResOpr resOpr = resOprs.get(i);
//				authinfos = new HashMap();
//				authinfos.put("roleId", roleId);
//				authinfos.put("resCode", resOpr.getResCode());
//				authinfos.put("resourceType", resourceType);
//				authinfos.put("roleType", roleType);
//				authinfos.put("permissionTable", permissionTable);
//				deleteAuths.add(authinfos);
//			}
//			this.executor.deleteBeans("cleanroleAuths", deleteAuths);
			
			List<Map> addAuths = new ArrayList<Map>();
			for(int i = 0; globalopcode !=null && i < globalopcode.length; i ++){
				authinfos = new HashMap();
				authinfos.put("roleId", roleId);
				authinfos.put("resourceType", resourceType);
				authinfos.put("roleType", roleType);
				authinfos.put("resCode", globalresourceid);
				authinfos.put("resName", "全局资源");
				authinfos.put("opCode", globalopcode[i]);
				authinfos.put("permissionTable", permissionTable);
				addAuths.add(authinfos);
			}
			
			for(int i = 0; resOprs !=null && i < resOprs.size(); i ++){
				authinfos = new HashMap();
				ResOpr resOpr = resOprs.get(i);
				authinfos.put("roleId", roleId);
				authinfos.put("resourceType", resourceType);
				authinfos.put("roleType", roleType);
				authinfos.put("resCode", resOpr.getResCode());
				authinfos.put("resName", resOpr.getResName());
				authinfos.put("opCode", resOpr.getOp());
				authinfos.put("permissionTable", permissionTable);
				addAuths.add(authinfos);
			}
			this.executor.insertBeans("addAuths", addAuths);
			tm.commit();
		}
		catch(Exception e)
		{
			throw new RoleException(e);
		}
		finally
		{
			tm.release();
		}
	}
	
	/** (non-Javadoc)
	 * @see com.frameworkset.platform.admin.service.RoleService#saveRoleAuths(java.lang.String, java.lang.String[], java.util.List, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void saveRoleAuths(List<ResOpr> resOprs, String resourceType,
			String roleId, String roleType,String permissionTable) throws RoleException  {
		TransactionManager tm = new TransactionManager();
		try
		{
			tm.begin();
			List<Map> addAuths = new ArrayList<Map>();
			for(int i = 0; resOprs !=null && i < resOprs.size(); i ++){
				HashMap authinfos = new HashMap();
				ResOpr resOpr = resOprs.get(i);
				authinfos.put("roleId", roleId);
				authinfos.put("resourceType", resourceType);
				authinfos.put("roleType", roleType);
				authinfos.put("resCode", resOpr.getResCode());
				authinfos.put("resName", resOpr.getResName());
				authinfos.put("opCode", resOpr.getOp());
				authinfos.put("permissionTable", permissionTable);
				int exit = this.executor.queryObjectBean(int.class, "hasgranted", authinfos);
				if(exit <= 0)
					addAuths.add(authinfos);
			}
			if(addAuths.size() > 0)
				this.executor.insertBeans("addAuths", addAuths);
			tm.commit();
		}
		catch(Exception e)
		{
			throw new RoleException(e);
		}
		finally
		{
			tm.release();
		}
	}
	/** (non-Javadoc)
	 * @see com.frameworkset.platform.admin.service.RoleService#getRequiredRoles(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public AuthRole[] getRequiredRoles(String resource, String action, String resourceType) throws RoleException {
 
		
		final List<AuthRole> authRoles = new ArrayList<AuthRole>();
		TransactionManager tm = new TransactionManager();
		AuthRole[] temp = null;
		try {
			tm.begin();
			ResourceInfo resourceInfo = this.resourceManager.getResourceInfoByType(resourceType);
			if(resourceInfo == null)
				throw new RoleException("getRequiredRoles failed:resource="+resource+",action="+  action+",resourceType="+  resourceType+",没有定义资源类型！");
			Map params = new HashMap();
			params.put("permissionTable", resourceInfo.getPermissionTable());
			params.put("resource", resource);
			params.put("action", action);
			params.put("resourceType", resourceType);
			executor.queryBeanByNullRowHandler(new NullRowHandler(){
				
				@Override
				public void handleRow(Record origine) throws Exception {
					String types= origine.getString("types");
					String role_id= origine.getString("role_id");	
					AuthRole role = new AuthRole();
					role.setRoleId(role_id);
					role.setRoleType(types);
					authRoles.add(role);
					 
				}
				
			}, "getRequiredRoles", params);
			if(authRoles.size() > 0)
			{
				
				List<AuthRole> rtemp = new ArrayList<AuthRole>();
				for(int i =0; i < authRoles.size(); i++){
					AuthRole role = authRoles.get(i);
					if(role.getRoleType().equals(AuthRole.TYPE_ROLE)){//设置角色名称
						String roleName = this.getSimpleRoleName(role.getRoleId());
						if(roleName == null || roleName.equals(""))
							continue;
						role.setRoleName(roleName);
					}
					else if(role.getRoleType().equals(AuthRole.TYPE_USER)){//设置用户名称						
						String userAccount = this.getSimpleUserAccount(role.getRoleId());
						if(userAccount == null || userAccount.equals(""))
							continue;
						role.setRoleName(userAccount);
					}
					rtemp.add(role);
						
				}
				temp = new AuthRole[rtemp.size()];	
				for(int i =0; i < rtemp.size(); i++){
					AuthRole role = rtemp.get(i);
					temp[i] = role;
						
				}
				 
			}
			tm.commit();
			return temp;
		} 
		catch(RoleException e){
			throw e;
		}
		catch (Exception e) {
			throw new RoleException("getRequiredRoles failed:resource="+resource+",action="+  action+",resourceType="+  resourceType,e);
		}
		finally
		{
			tm.releasenolog();
		}
		
	}
	/**
	 * @param roleId
	 * @return
	 */
	public String getSimpleUserAccount(String roleId) throws   RoleException{
		try {
			return this.executor.queryObject(String.class, "getSimpleUserAccount", roleId);
		} catch (SQLException e) {
			throw new RoleException("getSimpleUserAccount failed:"+roleId,e);
		}
	}
	/**
	 * @param roleId
	 * @return
	 * @throws SQLException 
	 */
	public String getSimpleRoleName(String roleId) throws   RoleException {		 
		try {
			return this.executor.queryObject(String.class, "getSimpleRoleName", roleId);
		} catch (SQLException e) {
			throw new RoleException("getSimpleRole failed:"+roleId,e);
		}
	}
	public boolean hasGrantedRoles(String resource, String resourceType)  throws RoleException{
		try {
			ResourceInfo resourceInfo = this.resourceManager.getResourceInfoByType(resourceType);
			if(resourceInfo == null)
				throw new RoleException("hasGrantedRoles failed:resource="+resource +",resourceType="+  resourceType+",没有定义资源类型！");
			Map params = new HashMap();
			params.put("permissionTable", resourceInfo.getPermissionTable());
			params.put("resource", resource);			 
			params.put("resourceType", resourceType);
			int num = this.executor.queryObjectBean(int.class, "hasGrantedRoles", params);
			return num > 0;
		}
		catch(RoleException e){
			throw e;
		}
		catch (SQLException e) {
			throw new RoleException("hasGrantedRoles failed:resourceType="+resourceType+",resource="+resource,e);
		}
	}
	
	public boolean hasGrantRole(AuthRole role, String resource, String resourceType)  throws RoleException {
		try {
			ResourceInfo resourceInfo = this.resourceManager.getResourceInfoByType(resourceType);
			if(role == null)
				throw new RoleException("hasGrantedRoles failed:resource="+resource +",resourceType="+  resourceType+",没有设置要判断的角色！");
			if(resourceInfo == null)
				throw new RoleException("hasGrantedRoles failed:resource="+resource +",resourceType="+  resourceType+",没有定义资源类型！");
			Map params = new HashMap();
			params.put("permissionTable", resourceInfo.getPermissionTable());
			params.put("resource", resource);			 
			params.put("resourceType", resourceType);
			params.put("roleId", role.getRoleId());
			params.put("roleType", role.getRoleType());
			int num = this.executor.queryObjectBean(int.class, "hasGrantRole", params);
			return num > 0;
		}
		catch(RoleException e){
			throw e;
		}
		catch (Exception e) {
			throw new RoleException("hasGrantRole failed:resourceType="+resourceType+",resource="+resource+role.toString(),e);
		}
	}
	
}