package sv.gob.bfa.middleware;

import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import sv.gob.bfa.middleware.processors.ProcessorJMSAttributes;

@SpringBootApplication
@ImportResource({"classpath:/spring-camel-context.xml"})
public class PSConsumerApplication {

	private static final Logger logger = Logger.getLogger(ProcessorJMSAttributes.class);
	
	public static void main(String[] args) {
		SpringApplication.run(PSConsumerApplication.class, args);
		logger.info("ENTORNO SPRING BOOT INICIO CORRECTAMENTE....//");//TODO: ISRAEL DEBUG
	}
}