package onlinejudge.contest.controller;

import java.security.Principal;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import onlinejudge.contest.service.ContestService;
import onlinejudge.domain.Contest;
import onlinejudge.domain.Problem;
import onlinejudge.domain.ProblemForContest;
import onlinejudge.domain.ProblemForTeam;
import onlinejudge.domain.Submit;
import onlinejudge.domain.Team;
import onlinejudge.dto.MyResponse;
import onlinejudge.message.util.MessageSourceUtil;


@Controller
public class ContestController implements MessageSourceAware{
	private final Logger logger = LoggerFactory.getLogger(ContestController.class);
	
	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	ContestService contestService;
	
	@RequestMapping({"/", "/about2"})
	public @ResponseBody String about(){
		return "Microservice Contest";
	}
	@RequestMapping("/about")
	public @ResponseBody Submit contest(){
		return new Submit();
	}
	/**
	 * #contest-001
	 * Create new Contest or update exist Contest
	 * @param contest
	 * @param principal
	 * @return
	 */
	@RequestMapping(value = "/contests", method=RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ResponseEntity<?> createContest(@RequestBody Contest contest, Principal principal){
		ResponseEntity<?> response = null;
		if(contest == null){
			logger.error("Contest is null");
			MyResponse myResponse = MyResponse.builder().fail().setObj(MessageSourceUtil.getMessage(messageSource, "contest.001.dataempty")).build();
			response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.BAD_REQUEST);
		}else{
			String errorMessageCheckAllField = "";
			if(StringUtils.isEmpty(contest.getShortName())){
				errorMessageCheckAllField = "Short name";
			}else if(StringUtils.isEmpty(contest.getLongName())){
				errorMessageCheckAllField = "Long name";
			}else if(StringUtils.isEmpty(contest.getDescription())){
				errorMessageCheckAllField = "Description";
			}else if(contest.getStartDate() == null){
				errorMessageCheckAllField = "Start Date";
			}else if(contest.getFreezeDate() == null){
				errorMessageCheckAllField = "Freeze Date";
			}else if(contest.getEndDate() == null){
				errorMessageCheckAllField = "End Date";
			}
			
			if(!errorMessageCheckAllField.isEmpty()){
				MyResponse myResponse = MyResponse.builder().fail().setObj(MessageSourceUtil.getMessage(messageSource, "contest.002.fieldempty", errorMessageCheckAllField)).build();
				response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.BAD_REQUEST);
			}else{
				Date startDate  = contest.getStartDate();
				Date freezeDate = contest.getFreezeDate();
				Date endDate = contest.getEndDate();
				if(startDate.equals(freezeDate) || startDate.after(freezeDate)){
					errorMessageCheckAllField = "contest.003.fieldempty" ; //"Start Date must be before Freeze Date";
				}else if(freezeDate.after(endDate)){
					errorMessageCheckAllField = "contest.004.fieldempty"; //"Freeze Date must be before End Date";
				}
				
				if(!errorMessageCheckAllField.isEmpty()){
					MyResponse myResponse = MyResponse.builder().fail().setObj(MessageSourceUtil.getMessage(messageSource,errorMessageCheckAllField)).build();
					response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.BAD_REQUEST);
				}else{
					contestService.updateContestAdminByEmail(contest, principal.getName());
					contest = contestService.saveContest(contest);
					logger.debug("Contest was save with id: " + contest.getId());
					response = new ResponseEntity<Contest>(contest, HttpStatus.OK);
				}
			}
		}
		return response;
	}
	/**
	 * #contest-002
	 * Get all Contest belong to currect user login (admin of contest)
	 * @param principal
	 * @return
	 */
	@RequestMapping(value = "/contests", method=RequestMethod.GET)
	public @ResponseBody List<Contest> getContests( Principal principal){
		List<Contest> listContest = contestService.getListContestCreateByAdminEmail(principal.getName());
		
		/**
		 * reduce data unnecessary
		 * - ProblemForContest
		 */
		
		for (Contest contest : listContest) {
			for (Team team : contest.getListTeam()) {
				for (ProblemForTeam problemForTeam : team.getListProblem()) {
					problemForTeam.setProblemForContest(null);
				}
			}
		}
		
		return listContest;
	}
	
	/**
	 * #contest-003
	 * Add Problem to Contest. List ProblemForContest will replace all old List ProblemForContest existing.
	 * @param contestID
	 * @param problemForContests
	 * @return
	 */
	@RequestMapping(value="/contests/{contestID}/problems", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ProblemForContest[] updateProblemForContest(@PathVariable String contestID,@RequestBody ProblemForContest[] problemForContests){
		return contestService.updateProblemForContest(contestID, problemForContests);
	}
	/**
	 * #contest-004
	 * Add Team to Contest. New Team will be appended in to List Team
	 * @param contestID
	 * @param team
	 * @return
	 */
	@RequestMapping(value="/contests/{contestID}/addteam", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Team addTeamToContest(@PathVariable String contestID,@RequestBody Team team){
		return contestService.addTeamToContest(contestID, team);
	}
	
	/**
	 * #contest-005
	 * Add or update Submit of team.
	 * @param submit
	 * @return
	 */
	@RequestMapping(value="/contests/team/submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Submit addSubmitToTeam(@RequestBody Submit submit){
		return contestService.addSubmitToTeam(submit);
	}
	
	/**
	 * #contest=006
	 * Get Problem by ProblemForContest's id.
	 * @param problemForContestId
	 * @return
	 */
	@RequestMapping(value ="/contests/problem_for_contest/problem",produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Problem getProblemByProblemForContestId(@RequestParam(required = true) String problemForContestId){ // problemId is problemForContestId
		return contestService.getProblemByProblemForContestId(problemForContestId);
	}
	
	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
	
}
