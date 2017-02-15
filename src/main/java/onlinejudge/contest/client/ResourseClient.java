package onlinejudge.contest.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import onlinejudge.dto.MyResponse;
import onlinejudge.dto.file.GroupResource;

@FeignClient(name = "onlinejudge-ms-resources")
@RequestMapping("/onlinejudge-ms-resources")
public interface ResourseClient {
	
	@RequestMapping(value = "/upfile", method=RequestMethod.POST)
	@ResponseBody
	public MyResponse upfile(@RequestBody GroupResource groupResource);
}
