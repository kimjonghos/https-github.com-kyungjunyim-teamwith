package com.teamwith.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.teamwith.dto.InterviewQuestionDTO;
import com.teamwith.service.ApplicationService;
import com.teamwith.service.MemberService;
import com.teamwith.service.TeamService;
import com.teamwith.util.Criteria;
import com.teamwith.vo.FaqVO;
import com.teamwith.vo.MemberSearchVO;
import com.teamwith.vo.MemberSimpleVO;
import com.teamwith.vo.MemberSkillVO;
import com.teamwith.vo.RecruitVO;
import com.teamwith.vo.TeamDetailVO;
import com.teamwith.vo.TeamRateVO;
import com.teamwith.vo.TeamSimpleVO;

@RequestMapping("/teamSearch")

@Controller
public class TeamSearchController {
	@Inject
	private TeamService teamService;
	@Inject
	private MemberService memberService;
	@Inject
	private ApplicationService applicationService;
	@Inject
	private MemberSimpleVO memberSimpleVO;

	@RequestMapping(value = "", method = RequestMethod.GET)
	public String teamSearch(HttpSession session, Model model, RedirectAttributes rttr) throws Exception {
		memberSimpleVO = (MemberSimpleVO) session.getAttribute("memberSimpleVO");

		List<TeamSimpleVO> recentTeamList = setRecentTeam();
		model.addAttribute("recentTeamList", recentTeamList);

		if (memberSimpleVO != null) {
			List<TeamRateVO> resultList = recommendTeam();
			model.addAttribute("recommendedTeamList", resultList);
		}
		
		return "teambuilding/jsp/teamSearch";
	}
	
	@RequestMapping(value="{teamId}", method=RequestMethod.GET)
	public String teamSearch(@PathVariable("teamId") String teamId, HttpSession session, Model model) throws Exception {
		memberSimpleVO = (MemberSimpleVO) session.getAttribute("memberSimpleVO");
		teamId = "team-" + teamId;
		
		boolean canApply = true;
		TeamDetailVO teamInfo = teamService.getTeamInfo(teamId);
		model.addAttribute("teamInfo", teamInfo);
		
		List<RecruitVO> recruitInfo = teamService.getRecruitInfo(teamId);
		model.addAttribute("recruitInfo", recruitInfo);
		
		List<InterviewQuestionDTO> interviewInfo = applicationService.getInterviewQuestion(teamId);
		model.addAttribute("interviewInfo", interviewInfo);

		List<MemberSearchVO> teamMembers = applicationService.getTeamMember(teamId);
		model.addAttribute("teamMembers", teamMembers);
		
		for (MemberSearchVO teamMember : teamMembers) {
			if (teamMember.getMemberId().equals(memberSimpleVO.getMemberId())) {
				canApply = false;
			}
		}
		
		model.addAttribute("canApply", canApply);
		
		List<FaqVO> faqInfo = teamService.getFaq(teamId);
		model.addAttribute("faqInfo", faqInfo);
		
		String teamEndDate = teamInfo.getTeamEndDate();
		int endYear = Integer.parseInt(teamEndDate.substring(0, 4));
		int endMonth = Integer.parseInt(teamEndDate.substring(5, 7));
		int endDate = Integer.parseInt(teamEndDate.substring(8, 10));
		GregorianCalendar endDay = new GregorianCalendar();
		endDay.set(endYear, endMonth, endDate);
		GregorianCalendar today = new GregorianCalendar();
		Date oneMonthAgo = today.getTime();
		oneMonthAgo.setMonth(oneMonthAgo.getMonth() + 1);
		today.setTime(oneMonthAgo);
		long dDay = (today.getTimeInMillis() / (24 * 60 * 60 * 1000))
				- (endDay.getTimeInMillis() / (24 * 60 * 60 * 1000));
		model.addAttribute("dDay", dDay);
		
		return "teambuilding/jsp/teamDetail";
	}

	private List<TeamSimpleVO> setRecentTeam() throws Exception {
		Criteria teamCri = new Criteria();
		return teamService.getRecentTeam(teamCri);
	}

	private List<TeamRateVO> recommendTeam() throws Exception {
		String memberId = memberSimpleVO.getMemberId();
		Map<String, Double> resultMap = new HashMap<String, Double>();
		List<String> teamIdListByRegion = null;
		List<String> teamIdListByRole = null;
		List<String> teamIdListByCategory = null;
		List<String> teamIdListBySkill = null;

		MemberSearchVO memberSearchVO = memberService.getMemberSearchInfo(memberId);
		List<String> memberProjectCategories = memberService.getMemberProjectCategory(memberId);
		MemberSkillVO memberSkillVO = memberService.getMemberSkill(memberId);

		String memberRole = memberSearchVO.getRoleId();
		String memberRegion1 = memberSearchVO.getRegionId1();
		String memberRegion2 = memberSearchVO.getRegionId2();

		Criteria regionCri = new Criteria();
		List<String> regionList = new ArrayList<String>();
		regionList.add(memberRegion1);
		regionList.add(memberRegion2);
		regionCri.addCriteria("regionList", regionList);

		teamIdListByRegion = teamService.searchTeam(regionCri);

		if (teamIdListByRegion == null) {
			return null;
		} else {
			for (String teamId : teamIdListByRegion) {
				if (resultMap.get(teamId) != null) {
					Double temp = resultMap.get(teamId);
					resultMap.put(teamId, temp + 1.0);
				} else {
					resultMap.put(teamId, 1.0);
				}
			}
		}

		Criteria categoryCri = new Criteria();
		categoryCri.addCriteria("projectCategoryList", memberProjectCategories);

		teamIdListByCategory = teamService.searchTeam(categoryCri);

		if (teamIdListByCategory != null) {
			for (String teamId : teamIdListByCategory) {
				if (resultMap.get(teamId) == null) {
					continue;
				} else {
					Double temp = resultMap.get(teamId);
					resultMap.put(teamId, temp + 1.0);
				}
			}
		}

		List<String> roleList = new ArrayList<String>();
		roleList.add(memberRole);
		Criteria roleCri = new Criteria();
		roleCri.addCriteria("roleList", roleList);

		teamIdListByRole = teamService.searchTeamDTO(roleCri);

		if (teamIdListByRole != null) {
			for (String teamId : teamIdListByRole) {
				if (resultMap.get(teamId) == null) {
					continue;
				} else {
					Double temp = resultMap.get(teamId);
					if (temp < 3.0) {
						resultMap.put(teamId, temp + 1.0);
					}
				}
			}
		}

		List<String> skillList = new ArrayList<String>();
		String[] skillMap = memberSkillVO.getSkill();
		
		for(String skill : skillMap) {
			skillList.add(skill);
		}
		
		int skillSize = skillList.size();

		Criteria skillCri = new Criteria();
		skillCri.addCriteria("skillList", skillList);

		teamIdListBySkill = teamService.searchTeamDTO(skillCri);

		if (teamIdListBySkill != null) {
			Map<String, Integer> countMap = new HashMap<String, Integer>();
			for (String teamId : teamIdListBySkill) {
				if (countMap.get(teamId) == null) {
					countMap.put(teamId, 1);
				} else {
					int temp = countMap.get(teamId);
					temp += 1;
					countMap.put(teamId, temp);
				}
			}

			Iterator<String> skillIterator = countMap.keySet().iterator();
			while (skillIterator.hasNext()) {
				String key = skillIterator.next();
				if (resultMap.get(key) != null) {
					double temp = resultMap.get(key);
					temp += (double) countMap.get(key) / (double) skillSize;
					resultMap.put(key, temp);
				}
			}
		}

		List<String> resultTeamId = new ArrayList<String>();
		Iterator<String> resultIterator = resultMap.keySet().iterator();
		while (resultIterator.hasNext()) {
			String key = resultIterator.next();
			double temp = resultMap.get(key);
			temp = temp / 4 * 100;
			resultMap.put(key, temp);
			resultTeamId.add(key);
		}

		Map<String, Double> sortedMap = sortByComparator(resultMap);
		List<TeamRateVO> resultList = new ArrayList<TeamRateVO>();

		Iterator<String> sortedIterator = sortedMap.keySet().iterator();
		while (sortedIterator.hasNext()) {
			String key = sortedIterator.next();
			TeamSimpleVO tempTeamSimpleVO = null;
			try {
				tempTeamSimpleVO = teamService.getTeamSimple(key);
			} catch (Exception e) {
				e.printStackTrace();
			}
			TeamRateVO teamRateVO = new TeamRateVO();

			teamRateVO.setTeamId(tempTeamSimpleVO.getTeamId());
			teamRateVO.setTeamPic(tempTeamSimpleVO.getTeamPic());
			teamRateVO.setTeamProjectName(tempTeamSimpleVO.getTeamProjectName());
			teamRateVO.setProjectCategoryId(tempTeamSimpleVO.getProjectCategoryId());
			teamRateVO.setTeamName(tempTeamSimpleVO.getTeamName());
			teamRateVO.setTeamEndDate(tempTeamSimpleVO.getTeamEndDate());
			teamRateVO.setTeamUpdateDate(tempTeamSimpleVO.getTeamUpdateDate());
			teamRateVO.setMemberId(tempTeamSimpleVO.getMemberId());
			teamRateVO.setMemberName(tempTeamSimpleVO.getMemberName());
			teamRateVO.setRate(sortedMap.get(key));

			resultList.add(teamRateVO);
		}

		return resultList;

	}

	private static Map<String, Double> sortByComparator(Map<String, Double> unsortMap) {

		List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(unsortMap.entrySet());

		// Sorting the list based on values
		Collections.sort(list, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});

		// Maintaining insertion order with the help of LinkedList
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}
}
