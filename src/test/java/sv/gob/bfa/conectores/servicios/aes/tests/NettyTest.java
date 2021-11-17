package sv.gob.bfa.conectores.servicios.aes.tests;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Properties;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.ISO87BPackager;
import org.junit.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import oracle.jdbc.pool.OracleDataSource;
import sv.gob.bfa.conectores.servicios.aes.dto.ConectoresServiciosAESPeticion;
import sv.gob.bfa.conectores.servicios.aes.dto.ConectoresServiciosAESRespuesta;
import sv.gob.bfa.conectores.servicios.aes.ex.dto.DatosRespuesta;
import sv.gob.bfa.soporte.comunes.exception.ServiceException;

public class NettyTest  extends CamelTestSupport{
	
	@Override
	public RoutesBuilder createRouteBuilder() throws Exception {
		return new ConectoresServiciosAESRutaNetty();
	}

	@Override
	protected CamelContext createCamelContext() throws Exception {
		CamelContext camelContext = super.createCamelContext();
		camelContext.addComponent("activemq",ActiveMQComponent.activeMQComponent("vm://localhost?broker.persistent=false"));
		return camelContext;
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
		
		jndi.bind("dataSourceMadminDs", database);
		jndi.bind("PROPAGATION_REQUIRES_NEW", PROPAGATION_REQUIRES_NEW);
		
		return jndi;
	}
	
	
	@Test
	public void testDirect() throws InterruptedException {

		try {
			
			context.start();
			
			ConectoresServiciosAESPeticion peticion = new ConectoresServiciosAESPeticion();
			
			peticion.setCodAgencia("0000");
			peticion.setCodSucursal("1234");
			peticion.setCodOrigen(1234);
			peticion.setCodEmpresa("4321");
			peticion.setFechaTransaccion(new Long(123456789));
			peticion.setMetodo("Consultar_Saldo");
			peticion.setMonto(new BigDecimal(125.50));
			peticion.setNumDocumento(new Long(987654321));
			peticion.setNumIdentificador("2225960");
			peticion.setPagoAlcaldia(0);
			peticion.setPagoReconexion(0);
			
			Exchange senderExchange = new DefaultExchange(context, ExchangePattern.InOut);
			senderExchange.getIn().setBody(peticion);
			System.out.println("Empezando");

			Exchange exchange = template.send("direct:conectores.servicios.aes", senderExchange);
			Message out = exchange.getOut();
			
			assertNotNull("No hay respuesta", out);
			
//			if(out.getBody() != null) {
//				ConectoresServiciosAESRespuesta respuesta1 = (ConectoresServiciosAESRespuesta) out.getBody();
//				if (respuesta1 != null) {
//					ObjectMapper mapper = new ObjectMapper();
//					String jsonString1 = mapper.writeValueAsString(respuesta1);
//					System.out.println("Respuesta: "+jsonString1);
//				}
//			}
			
			System.out.println("Terminamos.");
			context.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public class ConectoresServiciosAESRutaNetty extends RouteBuilder {
		
		private static final String OPERACION_CONSULTA_PAGO = "Consultar_Saldo";
		private static final String OPERACION_APLICAR_PAGO = "Aplicar_Pago";
		private static final String OPERACION_ANULAR_PAGO = "Anular_Pago";
		
		private static final String ENDPOINT = "//localhost:11000";
		
		//ISOMsg
		private final static String CODIGO_TERMINAL = "0000";
		private final static String ID_BANCO = "0000602300";
		private final static String TPDU = "600001000A";
		
		private final static String TIPO_SOLICITUD_CONSULTA = "0100";
		private final static String TIPO_SOLICITUD_PAGO = "0200";
		private final static String TIPO_SOLICITUD_REVERSION = "0420";
		
		@Override
		public void configure() throws Exception {
			
			onException(Exception.class)
				.routeId("conectores.servicios.aes.exception.handler")
				.handled(true)
				.log(LoggingLevel.ERROR,"Ocurrio un error: ${exception.message}")
				.process("")
				.choice()
					.when(simple("${header.tipoRespuesta} == 'jms'"))
						.marshal().json(JsonLibrary.Jackson)
				.end();

			from("direct:conectores.servicios.aes")
				.routeId("conectores.servicios.aes.direct")
				.setProperty("originalRequest",body())
				.setProperty("hostProperty",simple("{{config.host}}"))
				.setProperty("portProperty",simple("{{config.port}}"))
				.process(new Processor() {
					
					@Override
					public void process(Exchange exchange) throws Exception {
						
						//Validar parametros segun peticion
						
						Message in = exchange.getIn();
						
						ConectoresServiciosAESPeticion peticion = (ConectoresServiciosAESPeticion) in.getBody();
						
						if(peticion == null || peticion.getMetodo() == null || peticion.getNumIdentificador() == null
											|| peticion.getMetodo().isEmpty() || peticion.getNumIdentificador().isEmpty()
								) {
							throw new ServiceException(10,"Datos incompletos para realizar operacion.");
						}
						
						if(OPERACION_APLICAR_PAGO.equals(peticion.getMetodo())
							&& (peticion.getFechaTransaccion() == null)
							&& (peticion.getCodEmpresa() == null || peticion.getCodEmpresa().isEmpty())
							&& (peticion.getCodAgencia() == null || peticion.getCodAgencia().isEmpty())
							&& (peticion.getCodSucursal() == null || peticion.getCodSucursal().isEmpty())
							&& (peticion.getPagoAlcaldia() == null )
							&& (peticion.getPagoReconexion() == null )
							&& (peticion.getCodOrigen() == null )
							&& (peticion.getNumDocumento() == null )
							&& (peticion.getMonto() == null)
							)  throw new ServiceException(10, "Datos incompletos para realizar operacion.");
						
						if(OPERACION_ANULAR_PAGO.equals(peticion.getMetodo())
								&& (peticion.getCodEmpresa() == null || peticion.getCodEmpresa().isEmpty())
								&& (peticion.getCodAgencia() == null || peticion.getCodAgencia().isEmpty())
								&& (peticion.getCodSucursal() == null || peticion.getCodSucursal().isEmpty())
								&& (peticion.getNumDocumento() == null )
								)  throw new ServiceException(10, "Datos incompletos para realizar operacion.");
						
						//Preparando peticion de acuerdo al metodo de la peticion
						
						byte[] requestBytes = null;
						if(OPERACION_CONSULTA_PAGO.equals(peticion.getMetodo())){
							requestBytes = crearPeticionConsulta(TIPO_SOLICITUD_CONSULTA,CODIGO_TERMINAL,ID_BANCO,TPDU,peticion);
							
						}else if(OPERACION_APLICAR_PAGO.equals(peticion.getMetodo())) {
							requestBytes = crearPeticionPago(TIPO_SOLICITUD_PAGO, CODIGO_TERMINAL, ID_BANCO, TPDU, peticion);

						}else if(OPERACION_ANULAR_PAGO.equals(peticion.getMetodo())) {
							requestBytes = crearPeticionAnularPago(TIPO_SOLICITUD_REVERSION,CODIGO_TERMINAL,ID_BANCO,TPDU, peticion);
						}
						
						if(requestBytes == null) throw new ServiceException(999999, "Ocurrio un error inesperado.");
			             
						//Creando peticion
						
						in.setBody(requestBytes);
						
					}
				})
				
				//Invocando el servicio
				.to("netty4:tcp:"+ ENDPOINT + "?synchronous=true")
				
				//Creando respuesta final
				.process(new Processor() {
					
					@Override
					public void process(Exchange exchange) throws Exception {
						// TODO Crear proceso para recibir la respuesta
						
						ConectoresServiciosAESPeticion peticion = (ConectoresServiciosAESPeticion) exchange.getProperty("originalRequest");
						
						ConectoresServiciosAESRespuesta respuesta = new ConectoresServiciosAESRespuesta();
						
						Message in = exchange.getIn();
						
						if(in.getBody() == null ) throw new ServiceException(999999, "No se obtuvo respuesta del servicio.");
						
						byte[] responseBytes = (byte[]) in.getBody();
						
						DatosRespuesta respuestaAES = getDatosRespuesta(responseBytes,TPDU);
						
						respuesta.setCodigo(respuestaAES.getCodigo());
						respuesta.setDescripcion(respuestaAES.getMensajeError());
						
						String data = respuestaAES.getData();
						
						if(respuesta.getCodigo()==0) {
							
							if(OPERACION_CONSULTA_PAGO.equals(peticion.getMetodo())) {
								String fechaVencimiento = data.substring(31, 39).trim();
								if(fechaVencimiento.equals("")) {
									respuesta.setFechaVencimiento(0);
								} else {
									respuesta.setFechaVencimiento(Integer.parseInt(fechaVencimiento));
								}
								respuesta.setNombreCliente(data.substring(39, 74).trim());
								respuesta.setCodEmpresa(data.substring(74,78));//TODO Cod(4)+Nombre Empresa;
								respuesta.setEmpresa(data.substring(78, 89).trim());
								respuesta.setSaldoEnergia(new BigDecimal(data.substring(89, 101)));
								respuesta.setSaldoAlcaldia(new BigDecimal(data.substring(101, 113)));
								respuesta.setSaldoReconexion(new BigDecimal(data.substring(113, 125)));
							}else if(OPERACION_APLICAR_PAGO.equals(peticion.getMetodo())) {
								respuesta.setNumReferenciaTransaccionBanco(data.substring(51, 54));//Número de Referencia de Transacción del Banco
								respuesta.setNumeroAprobacionAES(data.substring(65, 80).trim());
							}else if(OPERACION_ANULAR_PAGO.equals(peticion.getMetodo())) {
								respuesta.setNumReferenciaTransaccionBanco(data.substring(31, 46).trim());//TODO Tomado de numero transaccion
							}
							
						}
						
						exchange.getIn().setBody(respuesta);
					}
				})
			;
			
			
			from("activemq:conectores.servicios.aes")
				.routeId("conectores.servicios.aes.activemq")
				.unmarshal().json(JsonLibrary.Jackson,ConectoresServiciosAESPeticion.class)
				.to("direct:conectores.servicios.aes")
				.marshal().json(JsonLibrary.Jackson);
			
		}
		
		private ISOMsg crearPeticionPrincipal(String tipoSolicitud, String codTerminal, String idBanco) throws ISOException {
			ISOPackager packager = new ISO87BPackager();
			ISOMsg msg = new ISOMsg();
			msg.setPackager(packager);
			msg.set(new ISOField(0, tipoSolicitud));
			msg.set(new ISOField(3, "1"));//TODO siempre tendra este valor
			msg.set(new ISOField(11, "1"));//TODO siempre tendra este valor
			msg.set(new ISOField(12, ISODate.getTime(new Date())));
			msg.set(new ISOField(13, ISODate.getDate(new Date())));
			msg.set(new ISOField(41, codTerminal));
			msg.set(new ISOField(42, idBanco));
			return msg;
		}
		
		private byte[] crearPeticionConsulta(String tipoSolicitud, String codTerminal, String idBanco, String tpdu,
				ConectoresServiciosAESPeticion peticion) {
			
			byte[] requestBytes = null;
			ISOMsg msg;
			try {
				msg = crearPeticionPrincipal(tipoSolicitud, codTerminal, idBanco);

				if(peticion.getNumIdentificador().length()==8) {
					msg.set(new ISOField(48, aplFormNic(1, peticion.getNumIdentificador()) ));
				}else {
					msg.set(new ISOField(48, aplFormNic(2, peticion.getNumIdentificador()) ));
				}
				requestBytes = msg.pack();
				String requestISO = ISOUtil.hexString(requestBytes);
				requestISO = tpdu + requestISO;
				Integer longitud = requestISO.length();
				requestISO = Integer.toHexString(longitud) + requestISO;
				requestBytes = ISOUtil.hex2byte(requestISO);
			} catch (ISOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return requestBytes;
		}
		
		private byte[] crearPeticionPago(String tipoSolicitud, String codTerminal, String idBanco, String tpdu,
				ConectoresServiciosAESPeticion peticion) {
			
			byte[] requestBytes = null;
			ISOMsg msg = null;
			Integer tipoEntrada = 0;
			if(peticion.getNumIdentificador().length()==8) {
				tipoEntrada = 1;
			}else {
				tipoEntrada = 2;
			}
			try {
				msg = crearPeticionPrincipal(tipoSolicitud, codTerminal, idBanco);
				String field48 = aplFormNic(tipoEntrada,peticion.getNumIdentificador())
					+ peticion.getCodEmpresa().substring(0, 4)
					+ formatMonto(peticion.getMonto())//TODO getMontoTotal
					+ String.valueOf(peticion.getPagoAlcaldia())
					+ String.valueOf(peticion.getPagoReconexion())
					+ peticion.getCodOrigen()
					+ formatNumeroTransaccion(peticion.getNumDocumento());//TODO registroControl.getNumeroRegistro()
				msg.set(new ISOField(48, field48));
				requestBytes = msg.pack();
				String requestISO = ISOUtil.hexString(requestBytes);
				requestISO = tpdu + requestISO;
				Integer longitud = requestISO.length();
				requestISO = Integer.toHexString(longitud) + requestISO;
				requestBytes = ISOUtil.hex2byte(requestISO);
			} catch (ISOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return requestBytes;
		}
		
		private byte[] crearPeticionAnularPago(String tipoSolicitud, String codTerminal, String idBanco, String tpdu,
				ConectoresServiciosAESPeticion peticion) {
			
			byte[] requestBytes = null;
			ISOMsg msg = null;
			Integer tipoEntrada = 0;
			if(peticion.getNumIdentificador().length()==8) {
				tipoEntrada = 1;
			}else {
				tipoEntrada = 2;
			}
			try {
				msg = crearPeticionPrincipal(tipoSolicitud, codTerminal, idBanco);
				String field48 = aplFormNic(tipoEntrada,peticion.getNumIdentificador())
					+ formatNumeroTransaccion(peticion.getNumDocumento());
				msg.set(new ISOField(48, field48));
				requestBytes = msg.pack();
				String requestISO = ISOUtil.hexString(requestBytes);
				requestISO = tpdu + requestISO;
				Integer longitud = requestISO.length();
				requestISO = Integer.toHexString(longitud) + requestISO;
				requestBytes = ISOUtil.hex2byte(requestISO);
			} catch (ISOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return requestBytes;
		}
		
		public DatosRespuesta getDatosRespuesta(byte[] responseBytes,String tpdu) throws ISOException {
			String responseISO = ISOUtil.hexString(responseBytes);
			ISOPackager packager = new ISO87BPackager();
			responseISO = responseISO.substring(tpdu.length(), responseISO.length());//TODO Variable TPDU
			responseBytes = ISOUtil.hex2byte(responseISO);
			ISOMsg msg = new ISOMsg();
			msg.setPackager(packager);
			msg.unpack(responseBytes);
			DatosRespuesta datosRespuesta = new DatosRespuesta();
			datosRespuesta.setCodigo(Integer.parseInt(msg.getString(39)));
			datosRespuesta.setData(msg.getString(48));
			datosRespuesta.setMensajeError(msg.getString(44));
			return datosRespuesta;
		}
		
		private String aplFormNic(Integer tipoEntrada, String valor) {
			
			String nicFormat = Integer.toString(tipoEntrada) + StringUtils.rightPad(valor, 30, '0');
			
			return nicFormat;
			
		}
		
		private String formatNumeroTransaccion(Long numTransaccion) {
			
			String num = String.valueOf(numTransaccion);
			num = StringUtils.rightPad(num, 15, ' ');
			
			return num;
			
		}
		
		private String formatMonto(BigDecimal monto) {
			
			String montoForm = "";
			montoForm = String.valueOf(monto);
			montoForm = StringUtils.rightPad(montoForm, 12, '0');
			
			return montoForm;
		}
		
	}
	
}
