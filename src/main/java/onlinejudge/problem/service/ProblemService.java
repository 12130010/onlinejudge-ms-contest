package onlinejudge.problem.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import onlinejudge.contest.client.UserClient;
import onlinejudge.domain.Problem;
import onlinejudge.domain.TestCase;
import onlinejudge.file.dto.GroupResource;
import onlinejudge.file.dto.MyResource;
import onlinejudge.repository.ProblemRepository;

@Service
public class ProblemService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	ProblemRepository problemRepository;
	
	@Autowired
	UserClient userClient;
	
	/**
	 * Save problem
	 * 
	 * @param problem
	 * @return
	 */
	public Problem saveProblem(Problem problem){
		logger.debug(problem.toString());
		problem = problemRepository.save(problem);
		return problem;
	}
	
	public Problem getProblemById(String id){
		return problemRepository.findOne(id);
	}
	
	/**
	 * Check id of user (retrieve by email) is equals with Problem owner or not.
	 * @param email: email of user need to be checked.
	 * @return true if user is problem's owner.
	 */
	public boolean checkOwerOfProblemByEmail(String email, Problem problem){
		Map<String , Object> param = new HashMap<>();
		param.put("email", email);
		String userId = userClient.getUserIDByEmail(param);
		return userId.equals(problem.getIdOwner());
	}
	
	public void updateResource(Problem problem, GroupResource groupResource){
		/**
		 * - when iterator list resource,if there are resource which's type is RESOURCE_TYPE_TESTCASE_INPUT or RESOURCE_TYPE_TESTCASE_OUTPUT
		 * we will create new instance TestCase
		 * 
		 * - after iterator list resource, testCase is not null, we update this testCase in to listTestCase of problem.
		 */
		TestCase testCase = null;
		for (MyResource resource : groupResource.getListResource()) {
			switch (resource.getResourceType()) {
			case MyResource.RESOURCE_TYPE_PROBLEM:
				problem.setFilePath(resource.getFileName());
				break;
			case MyResource.RESOURCE_TYPE_TESTCASE_INPUT:
				if(testCase == null){
					testCase = new TestCase();
					testCase.setIdProblem(problem.getId());
				}
				testCase.setInputFilePath(resource.getFileName());
				break;
			case MyResource.RESOURCE_TYPE_TESTCASE_OUTPUT:
				if(testCase == null){
					testCase = new TestCase();
					testCase.setIdProblem(problem.getId());
				}
				testCase.setOutputFilePath(resource.getFileName());
				break;
			default:
				break;
			}
		}
		if(testCase != null){
			if(testCase.getInputFilePath() == null || testCase.getInputFilePath().isEmpty()
			|| testCase.getOutputFilePath() == null || testCase.getOutputFilePath().isEmpty()){
				throw new IllegalArgumentException("InputFile or OutputFile in TestCase must exist together");
			}
			problem.clearAllTestCase(); //TODO delete testcase on DB.
			problem.addTestCase(testCase);
		}
	}
	/**
	 * update owner of problem by user's email
	 * @param email
	 * @param problem
	 */
	public void updateProblemOwerByEmail(String email, Problem problem){
		Map<String,Object> param = new HashMap<>();
		param.put("email", email);
		problem.setIdOwner(userClient.getUserIDByEmail(param));
	}
	
	public List<Problem> getListProblemCreateByUserId(String userId){
		 List<Problem>  list =problemRepository.findByIdOwner(userId);
		 if(list == null)
			 list = new ArrayList<>();
		 return list;
	}
	public List<Problem> getListProblemCreateByUserEmail(String email){
		Map<String , Object> param = new HashMap<>();
		param.put("email", email);
		String userId = userClient.getUserIDByEmail(param);
		
		return  getListProblemCreateByUserId(userId);
	}
}
