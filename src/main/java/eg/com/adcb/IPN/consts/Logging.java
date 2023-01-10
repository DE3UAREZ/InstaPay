package eg.com.adcb.IPN.consts;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class Logging   {
//    private static final Logger LOGGER = LoggerFactory.getLogger(Logging.class);
 //   private static Logger logger = Logger.getLogger(Logging.class);
private static Environment environment;

public Logging(Environment environment){
    this.environment=environment;
}

  //  @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        filterChain.doFilter(requestWrapper, responseWrapper);
        long timeTaken = System.currentTimeMillis() - startTime;

        String requestBody = getStringValue(requestWrapper.getContentAsByteArray(),
                request.getCharacterEncoding());
        String responseBody = getStringValue(responseWrapper.getContentAsByteArray(),
                response.getCharacterEncoding());
        if (response.getStatus()==200){
            info("FINISHED PROCESSING : METHOD={"+request.getMethod()+"}; REQUESTURI={"+request.getRequestURI()+"}; REQUEST PAYLOAD={"+requestBody+"}; RESPONSE CODE={"+response.getStatus()+"}; RESPONSE={"+responseBody+"}; TIM TAKEN={"+timeTaken+"}");
       }
//        else {
//            logger.info("FINISHED PROCESSING : METHOD={"+request.getMethod()+"}; REQUESTURI={"+request.getRequestURI()+"}; REQUEST PAYLOAD={"+requestBody+"}; RESPONSE CODE={"+response.getStatus()+"}; RESPONSE={"+responseBody+"}; TIM TAKEN={"+timeTaken+"}");
//        }
//        logger.debug("Log4jExample: A Sample Debug Message");
//        logger.info("Log4jExample: A Sample Info  Message");

//        LOGGER.warn("FINISHED PROCESSING : METHOD={}; REQUESTURI={}; REQUEST PAYLOAD={}; RESPONSE CODE={}; RESPONSE={}; TIM TAKEN={}",
//                request.getMethod(), request.getRequestURI(), requestBody, response.getStatus(), responseBody,
//                timeTaken);
//        LOGGER.warn(
//                "FINISHED PROCESSING : METHOD={}; REQUESTURI={}; REQUEST PAYLOAD={}; RESPONSE CODE={}; RESPONSE={}; TIM TAKEN={}",
//                request.getMethod(), request.getRequestURI(), requestBody, response.getStatus(), responseBody,
//                timeTaken);
//        LOGGER.info(
//                "FINISHED PROCESSING : METHOD={}; REQUESTURI={}; REQUEST PAYLOAD={}; RESPONSE CODE={}; RESPONSE={}; TIM TAKEN={}",
//                request.getMethod(), request.getRequestURI(), requestBody, response.getStatus(), responseBody,
//                timeTaken);
        responseWrapper.copyBodyToResponse();
    }
    public static void info(String log_text) {

        try {

            // attach a file to FileWriter
            FileWriter fw
                    = new FileWriter(environment.getProperty("InfoLog"), true);


            fw.write(new Timestamp(new java.util.Date().getTime()).toString() + "  | ");
            // read each character from string and write
            // into FileWriter
            for (int i = 0; i < log_text.length(); i++)
                fw.write(log_text.charAt(i));

            fw.write("\n");
            System.out.println("Successfully written");

            // close the file
            fw.close();
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    public static void warn(String log_text) {


        try {

            // attach a file to FileWriter
            FileWriter fw
                    = new FileWriter(environment.getProperty("ErrorLog"), true);


            fw.write(new Timestamp(new java.util.Date().getTime()).toString() + "  | ");
            // read each character from string and write
            // into FileWriter
            for (int i = 0; i < log_text.length(); i++)
                fw.write(log_text.charAt(i));

            fw.write("\n");
            System.out.println("Successfully written");

            // close the file
            fw.close();
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    public static void host(String log_text) {


        try {

        	String enableLog=environment.getProperty("enableHostLog");
        	
            // attach a file to FileWriter
        	if (enableLog.equalsIgnoreCase("true"))
        	{
            FileWriter fw
                    = new FileWriter(environment.getProperty("HostLog"), true);


            fw.write(new Timestamp(new java.util.Date().getTime()).toString() + "  | ");
            // read each character from string and write
            // into FileWriter
            for (int i = 0; i < log_text.length(); i++)
                fw.write(log_text.charAt(i));

            fw.write("\n");
            System.out.println("Successfully written");

            // close the file
            fw.close();
        	}
        } catch (Exception e) {
            e.getStackTrace();
        }
    }


    private String getStringValue(byte[] contentAsByteArray, String characterEncoding) {
        try {
            return new String(contentAsByteArray, 0, contentAsByteArray.length, characterEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }
}
