package com.teamwith.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.teamwith.service.ApplicationService;
import com.teamwith.vo.InterviewVO;
import com.teamwith.vo.MemberSimpleVO;
import com.teamwith.vo.MyApplicationVO;

@RequestMapping("/application")
@Controller
public class ApplicationController {
	@Inject
	private ApplicationService applicationService;
	@Inject
	private MemberSimpleVO memberSimpleVO;

	@RequestMapping(value = "/myApplication", method = RequestMethod.GET)
	public String home(Model model, HttpSession session) {
		memberSimpleVO = (MemberSimpleVO) session.getAttribute("memberSimpleVO");
		String memberId = memberSimpleVO.getMemberId();
		// 나의 지원 목록 가져오기
		List<MyApplicationVO> myApplicationList = applicationService.getMyApplication(memberId);
		model.addAttribute("myApplicationList", myApplicationList);

		// 지원 별로 인터뷰 목록 가져오기
		Map<String, List<InterviewVO>> interviewMap = new HashMap<String, List<InterviewVO>>();
		for (MyApplicationVO myApplication : myApplicationList) {
			List<InterviewVO> interviewVOList = applicationService.getMyInterview(myApplication.getApplicationId());
			interviewMap.put(myApplication.getApplicationId(), interviewVOList);
		}
		model.addAttribute("interviewMap", interviewMap);
		
		return "teambuilding/jsp/myApplication";
		
	}
	
	@RequestMapping(value="/cancel/{applicationId}", method=RequestMethod.GET)
	public String cancel(@PathVariable("applicationId") String applicationId) {
		applicationId = "application-" + applicationId;
		
		applicationService.changeApplicationStatus(3, applicationId);
		
		return "redirect:/application/myApplication";
	}

}
