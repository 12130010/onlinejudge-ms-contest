package onlinejudge.problem.controller;

import java.io.IOException;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import onlinejudge.contest.client.ResourseClient;
import onlinejudge.contest.client.UserClient;
import onlinejudge.domain.Problem;
import onlinejudge.dto.MyResponse;
import onlinejudge.file.dto.GroupResource;
import onlinejudge.file.dto.MyResource;
import onlinejudge.file.util.MyFileUtil;
import onlinejudge.problem.service.ProblemService;

@Controller
public class ProblemController implements MessageSourceAware{
	@Autowired
	private MessageSource messageSource;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	ResourseClient resourceClient;
	@Autowired
	UserClient userClient;
	
	@Autowired
	ProblemService problemService;
	
	
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
	
	/**
	 * #problem-001
	 * @param problem
	 * @return
	 */
	@RequestMapping(value = "/problems", method = RequestMethod.POST,consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ResponseEntity<?> createProblem(@RequestBody Problem problem, Principal principal){
		logger.debug("CreateProblem :" + problem);
		ResponseEntity<?> response = null;
		MyResponse myResponse = null;
		try {
			//update owner
			problemService.updateProblemOwerByEmail(principal.getName(), problem);
			problem.setCreatedDate(new Date());
			problem.setUpdateDate(new Date());
			problem.increaseVersion();
			problem = problemService.saveProblem(problem);
			response = new ResponseEntity<Problem>(problem, HttpStatus.OK);
			logger.info("Create problem success! - problem: " + problem.getName() +" , Id: " + problem.getId());
		}catch (Exception e) {
			e.printStackTrace();
			myResponse = MyResponse.builder().fail().setObj(e.getMessage()).build();
			response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.INTERNAL_SERVER_ERROR);
			logger.error("Create problem fail: " + problem.getName());
		}
		return response;
	}
	
	/**
	 * #problem-002
	 * @param request
	 * @return
	 */
	@RequestMapping(value="/problems/upfile")
	public @ResponseBody ResponseEntity<?> updateFileInProblem(MultipartHttpServletRequest request, Principal principal,Locale locale){
		MyResponse myResponse = null;
		ResponseEntity<?> response = null;
		
		String idProblem = request.getParameter("idProblem");
		logger.debug("updateFileInProblem :" + idProblem + " by user have email: " + principal.getName());
		if(idProblem == null || idProblem.equals("")){
			myResponse = MyResponse.builder().fail().setObj("idProblem must not be null").build();
			response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.BAD_REQUEST);
			logger.error("Can't update Problem with id = null");
		}else{
			//Check Is Problem exist with idProblem?
			Problem existProblem = problemService.getProblemById(idProblem);
			if(existProblem == null){
				// not exist
				myResponse = MyResponse.builder().fail().setObj(getMessage("problem.001.notfoud",idProblem)).build();
				response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.BAD_REQUEST);
				logger.error("Can't find Problem with idProblem is:" + idProblem);
			}else{// is exist.
				
				existProblem.increaseVersion();
				//Check this user is owner of this product.
				if(problemService.checkOwerOfProblemByEmail(principal.getName(), existProblem)){
					// current user is problem's owner
					MultipartFile problemFile = request.getFile("problemFile");
					MultipartFile testCaseInput = request.getFile("testCaseInput");
					MultipartFile testCaseOuput = request.getFile("testCaseOuput");
					
					GroupResource groupResource = new GroupResource();
					MyResource myResource= null;
					
					try {
						if(problemFile != null){
							myResource = new MyResource();
							myResource.setResourceType(MyResource.RESOURCE_TYPE_PROBLEM);
							myResource.setFileName(String.format("%s/problem_v%s.%s", idProblem,existProblem.getVersion(), MyFileUtil.getExtentionOfFIle(problemFile.getOriginalFilename())));
							myResource.setData(problemFile.getBytes());
							groupResource.add(myResource);
						}
						boolean isTestCaseInOutExist = true;// check testCaseInput and testCaseOuput must exist together or not/
						
						if(testCaseInput != null || testCaseOuput !=null){
							if(testCaseInput != null && testCaseOuput !=null){
								
								/*testCaseInput and testCaseOuput must not be null at the same time.*/
								
								//testCaseInput 
								myResource = new MyResource();
								myResource.setResourceType(MyResource.RESOURCE_TYPE_TESTCASE_INPUT);
								myResource.setFileName(String.format("%s/input_v%s.txt", idProblem, existProblem.getVersion()));
								myResource.setData(testCaseInput.getBytes());
								groupResource.add(myResource);
								
								//testCaseOuput
								myResource = new MyResource();
								myResource.setResourceType(MyResource.RESOURCE_TYPE_TESTCASE_OUTPUT);
								myResource.setFileName(String.format("%s/output_v%s.txt", idProblem,existProblem.getVersion()));
								myResource.setData(testCaseOuput.getBytes());
								groupResource.add(myResource);
									
								logger.debug("List in Group Resource: " + groupResource.getListResource().size());
								
							}else{
								logger.error("testCaseInput and testCaseOuput must be exist at the sametime.");
								myResponse = MyResponse.builder().fail().setObj(getMessage("problem.001.tcInOutNotNull")).build();
								response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.BAD_REQUEST);
								isTestCaseInOutExist = false;
							}
						}
						if(isTestCaseInOutExist){
							if(groupResource.getListResource().isEmpty()){
								//nothing to update
								myResponse = MyResponse.builder().fail().setObj(getMessage("problem.001.notThingUpdate")).build();
								response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.BAD_REQUEST);
							}else{
								// send file to Resource server.. 
								myResponse = resourceClient.upfile(groupResource);
								logger.error("Upfile to resource server with status: " + myResponse.getMessage());
								if(myResponse.getCode() == MyResponse.CODE_SUCCESS){
									problemService.updateResource(existProblem, groupResource);
									problemService.saveProblem(existProblem);
									myResponse = MyResponse.builder().success().setObj("").build();
									response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.OK);
								}else{
									
									// up file fail
									logger.error("Upfile to resource server with status: error. " + myResponse.getObj());
									myResponse = MyResponse.builder().fail().setObj(getMessage("problem.001.failUpLoad")).build();
									response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.INTERNAL_SERVER_ERROR);
								}
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
						logger.error(e.getMessage());
						myResponse = MyResponse.builder().fail().setObj(getMessage("problem.common.serverError", e.getMessage())).build();
						response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}else{
					// current user is not problem's owner
					myResponse = MyResponse.builder().fail().setObj(getMessage("problem.001.notOwner", principal.getName())).build();
					response = new ResponseEntity<MyResponse>(myResponse, HttpStatus.BAD_REQUEST);
					logger.error("Current user with email " +principal.getName() +" is not problem's owner");
				}
			}
		}
		return response;
	}
	
	//#problem-003
	//TODO Log,...
	@RequestMapping(value = "problems", method = RequestMethod.GET)
	public @ResponseBody List<Problem> getListProblemCreatedByCurrentUser(Principal principal){
		return problemService.getListProblemCreateByUserEmail(principal.getName());
	}
	
	private Object getMessage(String code, Object... arg){
		String message = messageSource.getMessage(code,arg , null);
		message = MessageFormat.format(message, arg);
		return new Object[]{code, message};
	}
}
