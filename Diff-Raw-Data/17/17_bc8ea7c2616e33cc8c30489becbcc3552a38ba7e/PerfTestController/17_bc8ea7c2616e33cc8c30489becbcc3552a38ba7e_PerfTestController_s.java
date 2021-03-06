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
 package org.ngrinder.perftest.controller;
 
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.apache.commons.lang.StringUtils;
 import org.ngrinder.common.controller.NGrinderBaseController;
 import org.ngrinder.common.util.JSONUtil;
 import org.ngrinder.model.User;
 import org.ngrinder.perftest.model.PerfTest;
 import org.ngrinder.perftest.model.ProcessAndThread;
 import org.ngrinder.perftest.service.PerfTestService;
 import org.ngrinder.script.service.FileEntryService;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.data.domain.Page;
 import org.springframework.data.domain.PageRequest;
 import org.springframework.stereotype.Controller;
 import org.springframework.ui.ModelMap;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestMethod;
 import org.springframework.web.bind.annotation.RequestParam;
 import org.springframework.web.bind.annotation.ResponseBody;
 
 /**
  * Performance Test Controller.
  * 
  * @author Mavlarn
  * @author JunHo Yoon
  */
 @Controller
 @RequestMapping("/perftest")
 public class PerfTestController extends NGrinderBaseController {
 
 	private static final Logger LOG = LoggerFactory.getLogger(PerfTestController.class);
 
 	@Autowired
 	private PerfTestService perfTestService;
 
 	@Autowired
 	private FileEntryService fileEntiryService;
 
 	// private static final int DEFAULT_TEST_PAGE_ZISE = 15;
 
 	/**
 	 * Get Performance test lists.
 	 * 
 	 * @param user
 	 *            user
 	 * @param model
 	 *            modelMap
 	 * @param isFinished
 	 *            only list finished project
 	 * @param pageable
 	 *            page
 	 * @return perftest/list
 	 */
 	@RequestMapping("/list")
 	public String getTestList(User user, @RequestParam(required = false) String query,
 			@RequestParam(required = false) boolean onlyFinished, @RequestParam(required = false) PageRequest pageable,
 			ModelMap model) {
 		// FIXME
 		// not to paging on server side for now. Get all tests and
 		// paging/sorting in page.
 		// if (pageable == null) {
 		// pageable = new PageRequest(0, DEFAULT_TEST_PAGE_ZISE);
 		// }
 		Page<PerfTest> testList = perfTestService.getPerfTestList(user, query, onlyFinished, pageable);
 		model.addAttribute("testListPage", testList);
 		model.addAttribute("onlyFinished", onlyFinished);
 		model.addAttribute("query", query);
 		model.addAttribute("page", pageable);
 		return "perftest/list";
 	}
 
 	/**
 	 * Get performance test detail on give perf test id
 	 * 
 	 * @param user
 	 *            user
 	 * @param model
 	 *            model
 	 * @param id
 	 *            performance test id
 	 * @return "perftest/detail"
 	 */
 	@RequestMapping("/detail")
 	public String getTestDetail(User user, @RequestParam(required = false) Long id, ModelMap model) {
 		PerfTest test = null;
 		if (id != null) {
 			test = perfTestService.getPerfTest(id);
 		}
 		model.addAttribute("test", test);
 		model.addAttribute("scriptList", fileEntiryService.getAllFileEntries(user));
 		return "perftest/detail";
 	}
 
 	@RequestMapping(value = "/create", method = RequestMethod.POST)
 	public String saveTest(User user, ModelMap model, PerfTest test) {
 		perfTestService.savePerfTest(test);
		return getTestList(user, null, false, null, model);
 	}
 
 	@RequestMapping(value = "/clone", method = RequestMethod.POST)
 	public String cloneTest(ModelMap model, PerfTest test) {
 		test.setId(null);
 		perfTestService.savePerfTest(test);
		return "perftest/list";
 	}
 
 	/**
 	 * Calculate vuser assignment policy based on request vuser number.
 	 * 
 	 * @param model
 	 * @param newVuser
 	 *            how many vusers whil be used.
 	 * @return JSON
 	 */
 	@RequestMapping(value = "/updateVuser")
 	public @ResponseBody
 	String updateVuser(@RequestParam int newVuser, ModelMap model) {
 		ProcessAndThread processAndThread = perfTestService.calcProcessAndThread(newVuser);
 		Map<String, Object> rtnMap = new HashMap<String, Object>(3);
 		rtnMap.put(JSON_SUCCESS, true);
 		rtnMap.put(PARAM_THREAD_COUNT, processAndThread.getThreadCount());
 		rtnMap.put(PARAM_PROCESS_COUNT, processAndThread.getProcessCount());
 		return JSONUtil.toJson(rtnMap);
 	}
 
 	@RequestMapping(value = "/deleteTest")
 	public @ResponseBody
 	String deleteTest(ModelMap model, @RequestParam Long id) {
 		perfTestService.deletePerfTest(id);
 		return JSONUtil.returnSuccess();
 	}
 
 	@RequestMapping(value = "/deleteTests")
 	public @ResponseBody
 	String deleteTests(ModelMap model, @RequestParam String ids) {
 		String[] idList = StringUtils.split(ids, ",");
 		for (String idStr : idList) {
 			long id = Long.valueOf(idStr);
 			perfTestService.deletePerfTest(id);
 		}
 		return JSONUtil.returnSuccess();
 	}
 
 	@RequestMapping(value = "/report")
 	public String getReport(ModelMap model, @RequestParam long testId) {
 		PerfTest test = perfTestService.getPerfTest(testId);
 		model.addAttribute("test", test);
 		return "perftest/report";
 	}
 
 	@RequestMapping(value = "/getReportData")
 	public @ResponseBody
 	String getReportData(ModelMap model, @RequestParam long testId, @RequestParam String dataType,
 			@RequestParam int imgWidth) {
 		List<String> reportData = null;
 		Map<String, Object> rtnMap = new HashMap<String, Object>(2);
 		try {
 			reportData = perfTestService.getReportData(testId, dataType, imgWidth);
 			rtnMap.put(JSON_SUCCESS, true);
 		} catch (IOException e) {
 			rtnMap.put(JSON_SUCCESS, false);
 			LOG.error("Get report data failed.", e);
 		}
 		rtnMap.put(dataType, reportData);
 		return JSONUtil.toJson(rtnMap);
 	}
 }
