 /**
  * Copyright (C) Rotterdam Community Solutions B.V.
  * http://www.rotterdam-cs.com
  *
  ***********************************************************************************************************************
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
  * the License. You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations under the License.
  */
 
 package com.rcs.portlet.slider.util;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Enumeration;
 import java.util.List;
 
 import javax.portlet.PortletPreferences;
 import javax.portlet.PortletRequest;
 import javax.portlet.PortletResponse;
 
 import com.liferay.portal.kernel.exception.PortalException;
 import com.liferay.portal.kernel.exception.SystemException;
 import com.liferay.portal.kernel.util.ParamUtil;
 import com.liferay.portal.kernel.util.Validator;
 import com.liferay.portal.theme.ThemeDisplay;
 import com.liferay.portlet.PortletPreferencesFactoryUtil;
 import com.rcs.portlet.slider.model.Slide;
 
 /**
  * @author Rajesh
  * 
  */
 public class SliderUtil {
 
 		public static List<Slide> getSlides(PortletRequest request,
 										PortletResponse response)
 										throws PortalException, SystemException {
 
 				String portletResource = ParamUtil.getString(request,
 						"portletResource");
 				PortletPreferences portletPreferences = getPreference(request,
 						portletResource);
 
 				List<Slide> slides = new ArrayList<Slide>();
 				Enumeration<String> prefMap = portletPreferences.getNames();
 
 				while (prefMap.hasMoreElements()) {
 
 						String slideId = prefMap.nextElement();
 						if (slideId.startsWith("slides_")) {
 								String[] values = portletPreferences.getValues(
 										slideId, null);
 								if (Validator.isNotNull(values)) {
 										Slide slide = getSlide(slideId, values);
 										slides.add(slide);
 								}
 						}
 				}
 
 				Collections.sort(slides, new DateComparator());
 
 				return slides;
 		}
 
 		public static PortletPreferences getPreference(PortletRequest request,
 										String portletResource)
 										throws PortalException, SystemException {
 
 				if (portletResource == null
 					|| portletResource.trim().equals("")) {
 						ThemeDisplay themeDisplay = (ThemeDisplay) request
 														.getAttribute("THEME_DISPLAY");
 						portletResource = themeDisplay.getPortletDisplay()
 														.getId();
 				}
 
 				return PortletPreferencesFactoryUtil.getPortletSetup(request,
 						portletResource);
 		}
 
 		public static String buildSlides(PortletRequest renderRequest,
 										PortletResponse renderResponse)
 										throws PortalException, SystemException {
 
 				List<Slide> slides = SliderUtil.getSlides(renderRequest,
 						renderResponse);
 
 				StringBuilder slidesBuilder = new StringBuilder();
 				for (Slide slide : slides) {
 						if (Validator.isNotNull(slide.getLink())) {
 								slidesBuilder.append("<a href=\"");
 								slidesBuilder.append(slide.getLink());
 								slidesBuilder.append("\">");
 						}
 						slidesBuilder.append("<img src=\"");
 						slidesBuilder.append(slide.getImageUrl());
 						slidesBuilder.append("\" ");
 						if (Validator.isNotNull(slide.getDesc())) {
 								slidesBuilder.append(" title=\"");
 								slidesBuilder.append(slide.getDesc());
 								slidesBuilder.append("\" ");
 						}
 						slidesBuilder.append("/>");
 						if (Validator.isNotNull(slide.getLink())) {
 								slidesBuilder.append("</a>");
 						}
 				}// end slides for
 
 				return slidesBuilder.toString();
 		}
 
 		public static String buildSettings(PortletRequest renderRequest,
 										PortletResponse renderResponse)
 										throws PortalException, SystemException {
 
 				PortletPreferences preferences = SliderUtil.getPreference(
 						renderRequest, null);
 
 				// Slides Animation
 				String effectSelectedValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_EFFECT, "random");
 				String slicesValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_SLICES, "15");
 				String boxColumnValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_BOX_COLUMN, "8");
 				String animationSpeedValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_ANIMATION_SPEED, "500");
 				String pauseTimeValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_PAUSE_TIME, "3000");
 				String startSlideValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_START_SLIDE, "0");
 				String randomSlideValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_RANDOM_SLIDE, "false");
 
 				// Slides Navigation
 				String directionNav = preferences.getValue(
 						SliderParamUtil.SETTINGS_DIRECTION_NAV, "true");
 				String prevTextValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_PREVIOUS_TEXT, "Prev");
 				String nextTextValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_NEXT_TEXT, "Next");
 				String autoHideNav = preferences.getValue(
 						SliderParamUtil.SETTINGS_AUTO_HIDE_NAV, "false");
 				String controlNavValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_CONTROL_NAV, "true");
 				String keyboardNavValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_KEYBOARD_NAV, "true");
 				String pauseOnHoverValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_PAUSE_ONHOVER, "true");
 				String manualAdvanceValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_MANUAL_ADVANCE, "false");
 				String opacityValue = preferences.getValue(
 						SliderParamUtil.SETTINGS_OPACTIY, "0.8");
 
 				StringBuilder settings = new StringBuilder();
 				settings.append("effect:'" + effectSelectedValue + "'");
 				settings.append(", slices:" + slicesValue);
 				settings.append(", boxCols:" + boxColumnValue);
 
 				if (Validator.isNull(animationSpeedValue))
 						settings.append(", animSpeed:" + animationSpeedValue);
 				if (Validator.isNull(pauseTimeValue))
 						settings.append(", pauseTime:" + pauseTimeValue);
 				if (Validator.isNull(startSlideValue)
 					&& Validator.isNumber(startSlideValue))
 						settings.append(", startSlide:" + startSlideValue);
 				settings.append(", randomStart:" + randomSlideValue);
 
 				settings.append(", directionNav:" + directionNav);
 				settings.append(", directionNavHide:" + autoHideNav);
 
 				if (Validator.isNull(prevTextValue))
 						settings.append(", prevText:'" + prevTextValue + "'");
 				if (Validator.isNull(nextTextValue))
 						settings.append(", nextText:'" + nextTextValue + "'");
 
 				settings.append(", controlNav:" + controlNavValue);
 				settings.append(", keyboardNav:" + keyboardNavValue);
 				settings.append(", pauseOnHover:" + pauseOnHoverValue);
 				settings.append(", manualAdvance:" + manualAdvanceValue);
 				settings.append(", captionOpacity:" + opacityValue);
 
 				return settings.toString();
 		}
 
		public static Slide getSlide(PortletRequest request, String slideId) {
 
				PortletPreferences preferences = request.getPreferences();
				String[] values = preferences.getValues(slideId, null);
 
 				if (Validator.isNotNull(values)) {
 						return getSlide(slideId, values);
 				}
 
 				return new Slide();
 		}
 
 		public static Slide getSlide(String slideId, String[] values) {
 
 				String title = values[0];
 				String link = values[1];
 				String desc = values[2];
 				String imageUrl = values[3];
 				String timeMillis = values[4];
 
 				Slide slide = new Slide(slideId, title, link, imageUrl, desc,
 										timeMillis, 0);
 				return slide;
 		}
 }
