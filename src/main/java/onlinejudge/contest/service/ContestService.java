package onlinejudge.contest.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import onlinejudge.contest.client.UserClient;
import onlinejudge.domain.Contest;
import onlinejudge.repository.ContestRepository;

@Service
public class ContestService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	ContestRepository contestRepository;
	
	@Autowired
	UserClient userClient;
	
	public Contest saveContest(Contest contest){
		if(contest.getNumberMemPerTeam() == 0){
			contest.setNumberMemPerTeam((byte) 1);
		}
		return contestRepository.save(contest);
	}
	/**
	 * update admin of Contest by admin's email
	 * @param email
	 * @param contest
	 */
	public void updateContestAdminByEmail(Contest contest, String emailAdmin ){
		Map<String,Object> param = new HashMap<>();
		param.put("email", emailAdmin);
		contest.setAdminID(userClient.getUserIDByEmail(param));
	}
	
	public List<Contest> getListContestCreateByAdminId(String adminId){
		List<Contest> list = contestRepository.findListContestByAdminID(adminId);
		if(list == null)
			list = new ArrayList<>();
		return list;
	}
	
	public List<Contest> getListContestCreateByAdminEmail(String adminEmail){
		Map<String , Object> param = new HashMap<>();
		param.put("email", adminEmail);
		String userId = userClient.getUserIDByEmail(param);
		return getListContestCreateByAdminId(userId);
	}
}
