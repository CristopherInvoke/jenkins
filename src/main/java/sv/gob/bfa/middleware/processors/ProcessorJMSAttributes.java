package sv.gob.bfa.middleware.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

/**

 * Clase que permite el setting de atributos particulares y customizado para el reencolamiento de mensajes
 * basado en situaciones de excepción controladas para desconexiones backend
 * @author: Red HAt
 * @version: 22/11/2020
 */
//@Service
public class ProcessorJMSAttributes implements Processor {

//	PRIVATE STATIC INT CONTADOR =0;
//	PRIVATE STATIC FINAL LONG EXPIRATION_TIME = 120000;
	private static final Logger logger = Logger.getLogger(ProcessorJMSAttributes.class);

	
	//@Override
	public void process(Exchange exchange)  {
//		contador++;
//		logger.info("CONTADOR CONSUMER: " + contador);
//		long JMSTimestamp = (Long) exchange.getIn().getHeader("JMSTimestamp");
//		long JMSExpiration = (Long) exchange.getIn().getHeader("JMSExpiration");
//		String JMSMessageID = (String) exchange.getIn().getHeader("JMSMessageID");
//		
//		if (JMSExpiration == 0) {
//			exchange.getIn().setHeader("JMSExpiration", JMSTimestamp + EXPIRATION_TIME);
//			exchange.getIn().setHeader("OriginIdMessage", JMSMessageID);	
//		}
//		exchange.getIn().setHeader("JMSPriority", 1);
	}
}
