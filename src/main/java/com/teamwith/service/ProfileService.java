package com.teamwith.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.Part;

import org.springframework.stereotype.Service;

import com.teamwith.dao.CareerDAO;
import com.teamwith.dao.LicenseDAO;
import com.teamwith.dao.MemberDAO;
import com.teamwith.dao.MemberTendencyDAO;
import com.teamwith.dto.CareerDTO;
import com.teamwith.dto.LicenseDTO;
import com.teamwith.dto.MemberDTO;
import com.teamwith.dto.MemberTendencyDTO;
import com.teamwith.util.CodeGenerator;
import com.teamwith.util.MailUtil;
import com.teamwith.util.UploadFileUtils;
import com.teamwith.vo.CareerVO;
import com.teamwith.vo.LicenseVO;
import com.teamwith.vo.MemberTendencyVO;
import com.teamwith.vo.MemberVO;

@Service
public class ProfileService {

	@Inject
	private CareerDAO careerDAO;
	@Inject
	private LicenseDAO licenseDAO;
	@Inject
	private MemberDAO memberDAO;
	@Inject
	private MemberTendencyDAO memberTendencyDAO;

	private String memberPicPath = "c:/teamwith/image/member";

	private ProfileService() {
	}

	public String searchMemberAccount(Map<String, String> map) throws Exception {
		String memId = memberDAO.searchMemberAccount(map);

		String result = "��ġ�ϴ� ������ �����ϴ�.";

		// ���̵� ������ ���� ��� -> ���̵� ã��
		if (map.get("memberID") == null || map.get("memberId").trim().equals("")) {
			if (memId != null) {
				result = "�ش� ������ ���̵�� " + memId.substring(0, memId.length() - 2) + "** �Դϴ�.";
			}
		}
		// ���̵� ������ �ִ� ��� -> ��й�ȣ ã��
		else {
			String memberId = map.get("memberID");
			if (memberId.equals(memId)) {
				// �̸��� �߼�
				String tempKey = CodeGenerator.generate();

				Map<String, String> newPassword = new HashMap<String, String>();
				newPassword.put("memberId", memberId);
				newPassword.put("newMemberPassword", tempKey);
				memberDAO.updateTempPassword(newPassword);

				MailUtil.send(map.get("memberEmail"), "teamwith�� �ӽú�й�ȣ�� �߱޵Ǿ����ϴ�. ",
						"�ӽú�й�ȣ�� " + tempKey + " �Դϴ�. " + "�α��� �� ��й�ȣ�� �� �ٲپ� �ּ���. ");
				result = "�ش� �̸��Ϸ� �ӽ� ��й�ȣ�� �߱��Ͽ����ϴ�.";
			}
		}

		return result;
	}

	public int updatePassword(Map<String, String> map) throws Exception {

		int result = memberDAO.updatePassword(map);
		return result;
	}

	public List<CareerVO> getMemberCareer(String memberId) throws Exception {
		List<CareerDTO> temp = careerDAO.searchCareer(memberId);
		List<CareerVO> result = new ArrayList<CareerVO>();
		if (temp != null) {
			for (CareerDTO dto : temp) {
				result.add(dto.toVO());
			}
		}
		return result;
	}

	public List<LicenseVO> getMemberLicense(String memberId) throws Exception {

		List<LicenseDTO> temp = licenseDAO.searchLicense(memberId);
		List<LicenseVO> result = new ArrayList<LicenseVO>();
		if (temp != null) {
			for (LicenseDTO dto : temp) {
				result.add(dto.toVO());
			}
		}
		return result;
	}

	/**
	 * ȸ���� ���̵� �������ڷ� �޾Ƽ� ȸ�� ������ �������� �޼ҵ�.<br>
	 * Parameter:<br>
	 * ȸ���� ���̵�<br>
	 * Returns:<br>
	 * #���̵� �����ϴ� ��� ȸ���� ������ ������ ��ȯ<br>
	 * �� �� null�� ��ȯ�Ѵ�.
	 */
	public MemberVO getMemberInfo(String memberId) throws Exception {
		MemberDTO dto = memberDAO.searchMemberInfo(memberId);
		if (dto == null) {
			return null;
		}
		MemberVO vo = dto.toVO();

		return vo;
	}

	public MemberVO getMyInfo(String memberId) throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("condition", "member_id");
		map.put("value", memberId);
		List<MemberDTO> dto = memberDAO.searchMemberWithCondition(map);
		if (dto == null) {
			return null;
		}
		MemberVO vo = dto.get(0).toVO();

		return vo;
	}

	public MemberTendencyVO getMemberTendency(String memberId) throws Exception {
		List<MemberTendencyDTO> temp = memberTendencyDAO.searchMemberTendency(memberId);
		MemberTendencyVO result = new MemberTendencyVO(temp);

		return result;
	}

	public int addMemberCareer(List<CareerVO> career) throws Exception {
		if (career == null) {
			return -1;
		}
		int result = -1;

		for (CareerVO c : career) {
			c.setCareerId(generateId(careerDAO.getId(), "career"));
			result = careerDAO.addCareer(c.toDTO());
			if (result != 1) {
				return result;
			}
		}
		return result;
	}

	public int removeMemberCareer(String memberId) throws Exception {
		if (memberId == null) {
			return -1;
		}
		int result = -1;

		result = careerDAO.removeCareerByMember(memberId);
		return result;
	}

	public int addMemberLicense(List<LicenseVO> license) throws Exception {
		if (license == null) {
			return -1;
		}
		int result = -1;

		for (LicenseVO l : license) {
			l.setLicenseId(generateId(licenseDAO.getId(), "license"));
			result = licenseDAO.addLicense(l.toDTO());
			if (result != 1) {
				return result;
			}
		}
		return result;
	}

	public int removeMemberLicense(String memberId) throws Exception {
		if (memberId == null) {
			return -1;
		}
		int result = -1;

		result = licenseDAO.removeLicenseByMember(memberId);
		return result;
	}

	public int updateMemberInfo(MemberVO member, Part memberPic) throws Exception {
		if (member == null) {
			return -1;
		}
		int result = -1;
		if (memberPic != null) {
			String picSavedName = UploadFileUtils.uploadFile(memberPicPath, member.getMemberId(), memberPic);
			member.setMemberPic(picSavedName);
		}
		result = memberDAO.updateMember(member.toDTO());
		return result;
	}

	public int updateMemberTendency(MemberTendencyVO memberTendency) throws Exception {
		if (memberTendency == null) {
			return -1;
		}
		int result = -1;
		for (MemberTendencyDTO m : memberTendency.toDTO()) {
			result = memberTendencyDAO.updateTendencyFigure(m);
			if (result != 1) {
				return -1;
			}
		}

		return result;
	}

	public int initTendency(String memberId) throws Exception {
		if (memberId == null) {
			return -1;
		}
		int result = -1;
		for (int i = 1; i < 6; i++) {
			result = memberTendencyDAO.addMemberTendency(new MemberTendencyDTO(memberId, "tendency-" + i, 5));
			if (result != 1) {
				return -1;
			}
		}
		return result;
	}

	public int removeMemberTendency(String memberId) throws Exception {
		if (memberId == null) {
			return -1;
		}
		int result = -1;
		result = memberTendencyDAO.removeMemberTendency(memberId);
		return result;
	}

	public String registerMember(MemberVO member) {
		if (member == null) {
			return null;
		}
		int result = -1;
		try {
			result = memberDAO.addMember(member.toDTO());
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (result != -1) {
			return member.getMemberId();
		} else {
			return null;
		}
	}

	public String registerAdminMember(MemberVO member) {
		if (member == null) {
			return null;
		}
		int result = -1;
		try {
			result = memberDAO.addAdminMember(member.toDTO());
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (result != -1) {
			return member.getMemberId();
		} else {
			return null;
		}
	}

	public int removeMember(String memberId) {
		if (memberId == null) {
			return -1;
		}
		int result = -1;
		try {
			result = memberDAO.updateNonMember(memberId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public int hideMember(String memberId) {
		if (memberId == null) {
			return -1;
		}
		int result = -1;
		try {
			result = memberDAO.updateMemberStatus(memberId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	private String generateId(List<String> id, String tableName) {
		if (id == null) {
			return tableName + "-" + 1;
		}

		int maxCnt = -1;

		for (String str : id) {
			if (maxCnt < Integer.parseInt(str.split("-")[1])) {
				maxCnt = Integer.parseInt(str.split("-")[1]);
			}
		}

		return tableName + "-" + (maxCnt + 1);
	}

}
