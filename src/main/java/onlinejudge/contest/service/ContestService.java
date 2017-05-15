package onlinejudge.contest.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import onlinejudge.contest.client.UserClient;
import onlinejudge.domain.Contest;
import onlinejudge.domain.Problem;
import onlinejudge.domain.ProblemForContest;
import onlinejudge.domain.ProblemForTeam;
import onlinejudge.domain.Submit;
import onlinejudge.domain.Team;
import onlinejudge.domain.User;
import onlinejudge.repository.ContestRepository;
import onlinejudge.repository.ProblemForContestRepository;
import onlinejudge.repository.ProblemRepository;
import onlinejudge.repository.SubmitRepository;
import onlinejudge.repository.TeamRepository;

@Service
public class ContestService {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	ContestRepository contestRepository;

	@Autowired
	ProblemRepository problemRepository;
	
	@Autowired
	TeamRepository teamReposotory;
	
	@Autowired
	SubmitRepository submitReposotory;
	
	@Autowired
	ProblemForContestRepository problemForContestRepository;
	
	@Autowired
	UserClient userClient;

	public Contest saveContest(Contest contest) {
		if (contest.getNumberMemPerTeam() == 0) {
			contest.setNumberMemPerTeam((byte) 1);
		}
		return contestRepository.save(contest);
	}

	/**
	 * update admin of Contest by admin's email
	 * 
	 * @param email
	 * @param contest
	 */
	public void updateContestAdminByEmail(Contest contest, String emailAdmin) {
		Map<String, Object> param = new HashMap<>();
		param.put("email", emailAdmin);
		contest.setAdminID(userClient.getUserIDByEmail(param));
	}

	public List<Contest> getListContestCreateByAdminId(String adminId) {
		List<Contest> list = contestRepository.findListContestByAdminID(adminId);
		if (list == null)
			list = new ArrayList<>();
		return list;
	}

	public List<Contest> getListContestCreateByAdminEmail(String adminEmail) {
		Map<String, Object> param = new HashMap<>();
		param.put("email", adminEmail);
		String userId = userClient.getUserIDByEmail(param);
		return getListContestCreateByAdminId(userId);
	}

	/**
	 * Update List ProblemForContest to Contest.
	 * 
	 * @param contestID
	 * @param listNewProblemForContest
	 */
	public ProblemForContest[] updateProblemForContest(String contestID, ProblemForContest[] listNewProblemForContest) {
		//update Problem in ProblemForContest
		for (ProblemForContest problemForContest : listNewProblemForContest) {
			problemForContest.setProblem(problemRepository.findOne(problemForContest.getProblem().getId()));
		}
		
		Contest contest = contestRepository.findOne(contestID);
		
		//clear all old problemForContest
		List<ProblemForContest> listOldProblemForContest = contest.getListProblem();
		for (ProblemForContest problemForContest : listOldProblemForContest) {
			problemForContestRepository.delete(problemForContest.getId());
		}
		
		//add new list ProblemForContest
		contest.setListProblem(Arrays.asList(listNewProblemForContest));

		contest = contestRepository.save(contest);
		
		//update problemForTeam with new listProblemForContest
		for (Team team : contest.getListTeam()) {
			team.updateListProblem(contest.getListProblem());
		}
		contest = contestRepository.save(contest);
		
		return listNewProblemForContest;
	}
	/**
	 * Append new Team to Contest
	 * @param contestID
	 * @param team
	 */
	public Team addTeamToContest(String contestID, Team team) {
		List<User> listUser = new ArrayList<>();
		
		User userTmp;
		for (User user : team.getListMember()) {
			Map<String, Object> param = new HashMap<>();
			param.put("email", user.getEmail());
			userTmp = userClient.getUserByEmail2(param);
			
			listUser.add(userTmp);
		}
		
		team.setListMember(listUser);
		
		Contest contest = contestRepository.findOne(contestID);
		contest.getListTeam().add(team);
		
		team.setIdContest(contestID);
		team.updateListProblem(contest.getListProblem());
		
		contest = contestRepository.save(contest);
		
		return contest.getListTeam().get(contest.getListTeam().size() -1 );
	}
	
	public void deleteTeamIncontest(String contestId, String teamID){
		Contest contest = contestRepository.findOne(contestId);
		Team team = null;
		
		for (Team t : contest.getListTeam()) {
			if(t.getId().equals(teamID)){
				team = t;
				break;
			}
		}
		
		contest.getListTeam().remove(team);
		
		contestRepository.save(contest);
		teamReposotory.delete(teamID);
	}
	
	/**
	 * Add Submit of ProblemForTeam to Team.
	 * @param submit
	 * @return
	 */
	public Submit addSubmitToTeam(Submit submit){
		boolean submitIsNew = StringUtils.isEmpty(submit.getId());
		submit = submitReposotory.save(submit);
		
		if(submit.isResolve()){ // check resolve Problem
			Team team = teamReposotory.findOne(submit.getIdTeam());
			ProblemForTeam problemForTeam = team.getProblemForTeamById(submit.getIdProblemForTeam());
			if(!problemForTeam.isResolve()){
				problemForTeam.setResolve(true);
				teamReposotory.save(team);
			}
		}
		
		if(submitIsNew){ // it was not be created. Add it to team and save again.
			Team team = teamReposotory.findOne(submit.getIdTeam());
			ProblemForTeam problemForTeam = team.getProblemForTeamById(submit.getIdProblemForTeam());
			problemForTeam.addSubmit(submit);
			teamReposotory.save(team);
		}
		
		return submit;
	}
	
	public Problem getProblemByProblemForContestId(String problemForContestId){
		ProblemForContest problemForContest = problemForContestRepository.findOne(problemForContestId);
		return problemForContest.getProblem();
	}

	public void deleteContest(String contestID) {
		Contest contest = contestRepository.findOne(contestID);
		
		teamReposotory.delete(contest.getListTeam());
		
		problemForContestRepository.delete(contest.getListProblem());
		
		contestRepository.delete(contest);
	}
	
	
	/**
	 * Get list contest in which, user is member of team.
	 * @param emailUser
	 * @return
	 */
	public List<Contest> getListContestUserAsMember(String emailUser){
		
		//list team in which user is member.
		List<Team> listTeamUserIn = teamReposotory.findTeamsByEmailOfMember(emailUser);
		
		
		List<String> listIdContest = new ArrayList<>();
		for (Team team : listTeamUserIn) {
			listIdContest.add(team.getIdContest());
		}
		
		return Lists.newArrayList(contestRepository.findAll(listIdContest));
	}
}
