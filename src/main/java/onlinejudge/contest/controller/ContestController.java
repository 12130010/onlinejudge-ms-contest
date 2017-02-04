package onlinejudge.contest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import onlinejudge.domain.Contest;


@Controller
public class ContestController {
	private final Logger logger = LoggerFactory.getLogger(ContestController.class);
	
	@RequestMapping({"/", "/about2"})
	public @ResponseBody String about(){
		return "Microservice Contest";
	}
	@RequestMapping("/about")
	public @ResponseBody Contest contest(){
		return new Contest();
	}
}
