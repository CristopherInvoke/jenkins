package sv.gob.bfa.conectores.servicios.aes.tests;

import java.math.BigDecimal;
import java.util.Properties;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import oracle.jdbc.pool.OracleDataSource;
import sv.gob.bfa.conectores.servicios.aes.dto.ConectoresServiciosAESPeticion;
import sv.gob.bfa.conectores.servicios.aes.dto.ConectoresServiciosAESRespuesta;
import sv.gob.bfa.conectores.servicios.aes.ruta.ConectoresServiciosAESRuta;
import sv.gob.bfa.soporte.comunes.exception.processor.ExceptionProcessor;

public class ConectoresServiciosAESRutaTest extends CamelTestSupport{
	
	@Override
	public RoutesBuilder createRouteBuilder() throws Exception {
		return new ConectoresServiciosAESRuta();
	}

	@Override
	protected CamelContext createCamelContext() throws Exception {
		
		CamelContext camelContext = super.createCamelContext();
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
		activeMQConnectionFactory.setBrokerURL("tcp://coruscant.bfa.local:35981");
		activeMQConnectionFactory.setUserName("admin");
		activeMQConnectionFactory.setPassword("administr4dor.1");
		ActiveMQComponent activeMQComponent = new ActiveMQComponent();
		activeMQComponent.setConnectionFactory(activeMQConnectionFactory);
		camelContext.addComponent("activemq",activeMQComponent);
		return camelContext;
		
//		CamelContext camelContext = super.createCamelContext();
//		camelContext.addComponent("activemq",ActiveMQComponent.activeMQComponent("vm://localhost?broker.persistent=false"));
//		return camelContext;
	}
	
	@Override
	public boolean isUseAdviceWith() {
		return true;
	}
	
	@Override
	 protected Properties useOverridePropertiesWithPropertiesComponent() {
      
		Properties props = new Properties();
		props.setProperty("config.host", "localhost");
		props.setProperty("config.port", "11000");
		props.setProperty("activemq.requestTimeout", "20s");
		
		
		return props;
  }

	@Override
	protected JndiRegistry createRegistry() throws Exception {
		JndiRegistry jndi = super.createRegistry();
		System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES", "*");
		OracleDataSource database;
		database = new OracleDataSource();
		database.setURL("jdbc:oracle:thin:@172.16.7.23:1524:BU1");
		database.setUser("MADMIN");
		database.setPassword("MADMIN");
		
		DataSourceTransactionManager txManager = new DataSourceTransactionManager(database);
		txManager.setDataSource(database);
		
		SpringTransactionPolicy PROPAGATION_REQUIRES_NEW = new SpringTransactionPolicy(txManager);
		PROPAGATION_REQUIRES_NEW.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
		
		ExceptionProcessor exceptionProcessor = new ExceptionProcessor();
		
		jndi.bind("dataSourceMadminDs", database);
		jndi.bind("PROPAGATION_REQUIRES_NEW", PROPAGATION_REQUIRES_NEW);
		jndi.bind("exceptionProcessor", exceptionProcessor);
		
		return jndi;
	}
	
	
//	@Test
	public void testDirect() throws InterruptedException {

		try {
			
			context.start();
			
			ConectoresServiciosAESPeticion peticion = new ConectoresServiciosAESPeticion();
			
//			peticion.setCodAgencia("0000");
//			peticion.setCodSucursal("1234");
//			peticion.setCodOrigen(01);
//			peticion.setCodEmpresa("1898");
//			peticion.setFechaTransaccion(new Long(20180520));
			peticion.setMetodo("Consultar_Saldo");
//			peticion.setMonto(new BigDecimal(1395));
//			peticion.setNumDocumento(new Long("617004000000000"));
			peticion.setNumIdentificador("3130113");
//			peticion.setPagoAlcaldia(0);
//			peticion.setPagoReconexion(0);
			
			Exchange senderExchange = new DefaultExchange(context, ExchangePattern.InOut);
			senderExchange.getIn().setBody(peticion);
			System.out.println("Empezando");

			Exchange exchange = template.send("activemeq:conectores.servicios.aes", senderExchange);
			Message out = exchange.getOut();
			
			assertNotNull("No hay respuesta", out);
			
			if(out.getBody() != null) {
				ConectoresServiciosAESRespuesta respuesta1 = (ConectoresServiciosAESRespuesta) out.getBody();
				if (respuesta1 != null) {
					ObjectMapper mapper = new ObjectMapper();
					String jsonString1 = mapper.writeValueAsString(respuesta1);
					System.out.println("Respuesta: "+jsonString1);
				}
			}
			
			System.out.println("Terminamos.");
			context.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
//	@Test
	public void testMqRoute() throws Exception {
		
		context.start();
		
		ConectoresServiciosAESPeticion peticion = new ConectoresServiciosAESPeticion();
		
//		peticion.setCodAgencia("0000");
//		peticion.setCodSucursal("1234");
//		peticion.setCodOrigen(01);
//		peticion.setCodEmpresa("1898");
//		peticion.setFechaTransaccion(new Long(20180520));
		peticion.setMetodo("Consultar_Saldo");
//		peticion.setMonto(new BigDecimal(1395));
//		peticion.setNumDocumento(new Long("617004000000000"));
		peticion.setNumIdentificador("3130111");
//		peticion.setPagoAlcaldia(0);
//		peticion.setPagoReconexion(0);
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonInString = mapper.writeValueAsString(peticion);
		
		Exchange senderExchange = new DefaultExchange(context, ExchangePattern.InOut);
		senderExchange.getIn().setBody(jsonInString);

		System.out.println("Empezando");

		Exchange exchange = template.send("activemq:conectores.servicios.aes", senderExchange);
		Message out = exchange.getOut();

		assertNotNull("No hay respuesta", out);
		
		System.out.println("Respuesta activemq: " + new String((byte[]) out.getBody()));
		
		context.stop();
	}
	
	@Test
	public void testPagoMqRoute() throws Exception {
		
		context.start();
		
		ConectoresServiciosAESPeticion peticion = new ConectoresServiciosAESPeticion();
		
//		peticion.setCodAgencia("0000");
//		peticion.setCodSucursal("1234");
//		peticion.setCodOrigen(01);
//		peticion.setCodEmpresa("1898");
//		peticion.setFechaTransaccion(new Long(20180520));
//		peticion.setMetodo("Aplicar_Pago");
//		peticion.setMonto(new BigDecimal("1417"));
//		peticion.setNumDocumento(new Long("0"));
//		peticion.setNumIdentificador("5112706");
//		peticion.setPagoAlcaldia(0);
//		peticion.setPagoReconexion(0);
		
		peticion.setCodAgencia("4");
		peticion.setCodSucursal("7");
		peticion.setCodOrigen(10);
		peticion.setCodEmpresa("2260");
		peticion.setFechaTransaccion(new Long("1545344277157"));
		peticion.setMetodo("Anular_Pago");
		peticion.setMonto(new BigDecimal("3401"));
		peticion.setNumDocumento(new Long("0"));
		peticion.setNumIdentificador("3130111");
		peticion.setPagoAlcaldia(1);
		peticion.setPagoReconexion(1);
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonInString = mapper.writeValueAsString(peticion);
		
		Exchange senderExchange = new DefaultExchange(context, ExchangePattern.InOut);
		senderExchange.getIn().setBody(jsonInString);

		System.out.println("Empezando");

		Exchange exchange = template.send("activemq:conectores.servicios.aes", senderExchange);
		Message out = exchange.getOut();

		assertNotNull("No hay respuesta", out);
		
		System.out.println("Respuesta activemq: " + new String((byte[]) out.getBody()));
		
		context.stop();
	}
	
}
