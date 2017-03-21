package onlinejudge.util;

import java.text.MessageFormat;

import org.springframework.context.MessageSource;

public class MessageSourceUtil {
	
	public static Object getMessage(MessageSource messageSource, String code, Object... arg){
		String message = messageSource.getMessage(code,arg , null);
		message = MessageFormat.format(message, arg);
		return new Object[]{code, message};
	}
	
}
