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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import onlinejudge.contest.service.ContestService;
import onlinejudge.domain.Contest;
import onlinejudge.dto.MyResponse;
import onlinejudge.util.MessageSourceUtil;


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
	public @ResponseBody Contest contest(){
		return new Contest();
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
		return contestService.getListContestCreateByAdminEmail(principal.getName());
	}
	
	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
