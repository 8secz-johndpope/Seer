 /*
  * Copyright (C) 2012 - 2012 NHN Corporation
  * All rights reserved.
  *
  * This file is part of The nGrinder software distribution. Refer to
  * the file LICENSE which is part of The nGrinder distribution for
  * licensing details. The nGrinder distribution is available on the
  * Internet at http://nhnopensource.org/ngrinder
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
  * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
  * COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
  * OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package org.ngrinder.user.controller;
 
 import static org.ngrinder.common.util.Preconditions.checkArgument;
 import static org.ngrinder.common.util.Preconditions.checkNotEmpty;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.EnumSet;
 import java.util.List;
 
 import org.apache.commons.lang.StringUtils;
 import org.ngrinder.common.controller.NGrinderBaseController;
 import org.ngrinder.common.util.JSONUtil;
 import org.ngrinder.model.Role;
 import org.ngrinder.model.User;
 import org.ngrinder.user.service.UserService;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.security.access.prepost.PreAuthorize;
 import org.springframework.stereotype.Controller;
 import org.springframework.ui.ModelMap;
 import org.springframework.web.bind.annotation.ModelAttribute;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestParam;
 import org.springframework.web.bind.annotation.ResponseBody;
 
 @Controller
 @RequestMapping("/user")
 public class UserController extends NGrinderBaseController {
 
 	@Autowired
 	private UserService userService;
 
 	@PreAuthorize("hasAnyRole('A', 'S')")
 	@RequestMapping("/list")
 	public String getUserList(ModelMap model, @RequestParam(required = false) String roleName,
 			@RequestParam(required = false) String keywords) {
 
 		List<User> userList = null;
 		if (StringUtils.isEmpty(keywords)) {
 			userList = userService.getAllUserByRole(roleName);
 		} else {
 			userList = userService.getUserListByKeyWord(keywords);
 			model.put("keywords", keywords);
 		}
 
 		model.addAttribute("userList", userList);
 		EnumSet<Role> roleSet = EnumSet.allOf(Role.class);
 		model.addAttribute("roleSet", roleSet);
 		model.addAttribute("roleName", roleName);
 
 		return "user/userList";
 	}
 
 	/**
 	 * Get user detail page.
 	 * 
 	 * @param model
 	 * @param userId
 	 * @return view name
 	 */
 
 	@RequestMapping("/detail")
 	@PreAuthorize("hasAnyRole('A', 'S') or #user.userId == #userId")
 	public String getUserDetail(User user, final ModelMap model, final @RequestParam(required = false) String userId) {
 
 		List<User> userList = userService.getAllUserByRole(null);
 		model.addAttribute("userList", userList);
 		EnumSet<Role> roleSet = EnumSet.allOf(Role.class);
 		model.addAttribute("roleSet", roleSet);
 
 		User retrievedUser = userService.getUserById(userId);
 
 		model.addAttribute("user", retrievedUser);
 		return "user/userDetail";
 	}
 
 	@RequestMapping("/save")
 	@PreAuthorize("hasAnyRole('A', 'S') or #user.id == #updatedUser.id")
 	public String saveOrUpdateUserDetail(User user, ModelMap model, @ModelAttribute("user") User updatedUser) {
 
 		checkArgument(updatedUser.validate());
 		// General user can not change their role.
		if (user.getRole() != Role.ADMIN || user.getRole() != Role.SUPER_USER) {
 			user.setRole(null);
 		}
 		if (updatedUser.exist()) {
 			userService.modifyUser(updatedUser);
 		} else {
 			userService.saveUser(updatedUser);
 		}
 		if (user.getId() == updatedUser.getId()) {
 			return "redirect:/";
 		} else {
 			return "redirect:/user/list";
 		}
 	}
 
 	@PreAuthorize("hasAnyRole('A', 'S')")
 	@RequestMapping("/delete")
 	public String deleteUser(ModelMap model, @RequestParam String userIds) {
 		String[] ids = userIds.split(",");
 		ArrayList<String> aListNumbers = new ArrayList<String>(Arrays.asList(ids));
 		userService.deleteUsers(aListNumbers);
 		return "redirect:/user/list";
 	}
 
 	@PreAuthorize("hasAnyRole('A', 'S')")
 	@RequestMapping("/checkUserId")
 	public @ResponseBody
 	String checkUserId(ModelMap model, @RequestParam String userId) {
 		User user = userService.getUserById(userId);
 		if (user == null) {
 			return JSONUtil.returnSuccess();
 		} else {
 			return JSONUtil.returnError();
 		}
 	}
 	
 	@RequestMapping("/profile")
 	public String userProfile(User user, ModelMap model) {
 		checkNotEmpty(user.getUserId(), "UserID should not be NULL!");
 		User newUser = userService.getUserById(user.getUserId());
 		model.addAttribute("user", newUser);
 		model.addAttribute("action", "profile");
 		return "user/userInfo";
 	}
 
 }
