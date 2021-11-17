package sv.gob.bfa.conectores.servicios.aes.ruta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Date;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.ISO87BPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import sv.gob.bfa.conectores.servicios.aes.dto.ConectoresServiciosAESPeticion;
import sv.gob.bfa.conectores.servicios.aes.dto.ConectoresServiciosAESRespuesta;
import sv.gob.bfa.conectores.servicios.aes.ex.dto.DatosRespuesta;
import sv.gob.bfa.conectores.servicios.aes.iso8583.IsoStreamReader;
import sv.gob.bfa.soporte.comunes.exception.ServiceException;

public class ConectoresServiciosAESRuta extends RouteBuilder {

	private static final String OPERACION_CONSULTA_PAGO = "Consultar_Saldo";
	private static final String OPERACION_APLICAR_PAGO = "Aplicar_Pago";
	private static final String OPERACION_ANULAR_PAGO = "Anular_Pago";
	
	//ISOMsg
	private final static String CODIGO_TERMINAL = "1212";
	private final static String ID_BANCO = "0000602200";
	private final static String TPDU = "600001000A";
	private final static String PARAM_3 = "000001";
	private final static String PARAM_11 = "1";
	
	private final static String TIPO_SOLICITUD_CONSULTA = "0100";
	private final static String TIPO_SOLICITUD_PAGO = "0200";
	private final static String TIPO_SOLICITUD_REVERSION = "0420";
	
	Logger logger = LoggerFactory.getLogger(ConectoresServiciosAESRuta.class);
	
	@Override
	public void configure() throws Exception {
		
		onException(Exception.class)
			.routeId("conectores.servicios.aes.exception.handler")
			.handled(true)
			.log(LoggingLevel.ERROR,"Ocurrio un error: ${exception.message}");
//TODO: 
//			.process("exceptionProcessor");
//			.choice()
//				.when(simple("${header.tipoRespuesta} == 'jms'"))
//					.marshal().json(JsonLibrary.Jackson)
//			.end();
		from("direct:conectores.servicios.aes")
			.routeId("conectores.servicios.aes.direct")
			.log(LoggingLevel.INFO, "INICIANDO SERVICIO CONECTOR AES...///")//DEBUG
			.setProperty("originalRequest",body())
			.log("funciona")
			.setProperty("hostProperty",simple("{{config.host}}"))
			.setProperty("portProperty",simple("{{config.port}}"))
			.setProperty("requestTimeoutProperty",simple("{{activemq.requestTimeout}}"))
			.log(LoggingLevel.INFO, "CREANDO PETICION WS...///")//DEBUG
			.process(new CrearPeticionProcessor())
			;
		
		
		from("activemq:conectores.servicios.aes")
			.routeId("conectores.servicios.aes.activemq")
			.log(LoggingLevel.INFO, "INICIANDO LA CONECCION CON EL AMQ...///")//TODO:  DEBUG conexion amq
			.unmarshal().json(JsonLibrary.Jackson,ConectoresServiciosAESPeticion.class)
			.to("direct:conectores.servicios.aes")
			.marshal().json(JsonLibrary.Jackson);
		
	}
	
	private class CrearPeticionProcessor implements Processor {

		@Override
		public void process(Exchange exchange) throws Exception {
			
			logger.info("dentro del process exchange");//TODO:DEBUG
			logger.info("body: "+exchange.getIn().getBody());//TODO:DEBUG
			
			//Validar parametros segun peticion
			
			Message in = exchange.getIn();
			
			ConectoresServiciosAESPeticion peticion = (ConectoresServiciosAESPeticion) in.getBody();
			
			if(peticion == null || peticion.getMetodo() == null || peticion.getNumIdentificador() == null
								|| peticion.getMetodo().isEmpty() || peticion.getNumIdentificador().isEmpty()
					) {
				throw new ServiceException(10,"Datos incompletos para realizar operacion.");
			}
			
			if(OPERACION_APLICAR_PAGO.equals(peticion.getMetodo()))
				
				if((peticion.getFechaTransaccion() == null)
				|| (peticion.getCodEmpresa() == null || peticion.getCodEmpresa().isEmpty())
				|| (peticion.getCodAgencia() == null || peticion.getCodAgencia().isEmpty())
				|| (peticion.getCodSucursal() == null || peticion.getCodSucursal().isEmpty())
				|| (peticion.getPagoAlcaldia() == null )
				|| (peticion.getPagoReconexion() == null )
				|| (peticion.getCodOrigen() == null )
				|| (peticion.getNumDocumento() == null )
				|| (peticion.getMonto() == null)
				)  throw new ServiceException(10, "Datos incompletos para realizar operacion.");
			
			if(OPERACION_ANULAR_PAGO.equals(peticion.getMetodo()) && (
					   (peticion.getCodEmpresa() == null || peticion.getCodEmpresa().isEmpty())
					|| (peticion.getCodAgencia() == null || peticion.getCodAgencia().isEmpty())
					|| (peticion.getCodSucursal() == null || peticion.getCodSucursal().isEmpty())
					|| (peticion.getNumDocumento() == null )
						)
					)  throw new ServiceException(10, "Datos incompletos para realizar operacion.");
			
			//Preparando peticion de acuerdo al metodo de la peticion
			
			String codTerminal = exchange.getContext().resolvePropertyPlaceholders("{{config.conector.aes.codigo.terminal}}");
			String idBanco = exchange.getContext().resolvePropertyPlaceholders("{{config.conector.aes.id.banco}}");
			String tpdu = exchange.getContext().resolvePropertyPlaceholders("{{config.conector.aes.tpdu}}");
			
			byte[] requestBytes = null;
			if(OPERACION_CONSULTA_PAGO.equals(peticion.getMetodo())){
				requestBytes = crearPeticionConsulta(TIPO_SOLICITUD_CONSULTA,codTerminal,idBanco,tpdu,peticion);
				
			}else if(OPERACION_APLICAR_PAGO.equals(peticion.getMetodo())) {
				requestBytes = crearPeticionPago(TIPO_SOLICITUD_PAGO,codTerminal,idBanco,tpdu, peticion);

			}else if(OPERACION_ANULAR_PAGO.equals(peticion.getMetodo())) {
				requestBytes = crearPeticionAnularPago(TIPO_SOLICITUD_REVERSION,codTerminal,idBanco,tpdu, peticion);
			}
			
			if(requestBytes == null) throw new ServiceException(1, "Ocurrio un error inesperado.");
             
			//Creando peticion
			
			logger.info("PETICION TRAMA -- > {}", ISOUtil.hexString(requestBytes));//DEBUG
			
			String host = exchange.getProperty("hostProperty",String.class);
			Integer port = exchange.getProperty("portProperty",Integer.class);
			String  requestTimeOutTemp=exchange.getProperty("requestTimeoutProperty", String.class);
			
			Integer requestTimeOut;
			
			if(requestTimeOutTemp.toLowerCase().contains("s"))
				requestTimeOut=1000*Integer.valueOf(requestTimeOutTemp.toLowerCase().replaceAll("s", ""));
			else 
				requestTimeOut=Integer.valueOf(requestTimeOutTemp);
			
			logger.info("PARAMETROS -- > requestTimeOut:"+requestTimeOut+" port:"+port+" host:"+host);//DEBUG
			
			Socket socket = null;

				socket = new Socket(host, port);
				socket.setSoTimeout(requestTimeOut);
				byte[] responseBytes = enviarTrama(socket, requestBytes);
				logger.info("PREPARANDO RESPONDER LA PETICION...:"+responseBytes);//DEBUG
				DatosRespuesta respuestaAES = getDatosRespuesta(responseBytes,tpdu);
				
				ConectoresServiciosAESRespuesta respuesta = new ConectoresServiciosAESRespuesta();
				
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
						respuesta.setSaldoEnergia(formatearBigDecimal(data.substring(89, 101)));
						respuesta.setSaldoAlcaldia(formatearBigDecimal(data.substring(101, 113)));
						respuesta.setSaldoReconexion(formatearBigDecimal(data.substring(113, 125)));
					}else if(OPERACION_APLICAR_PAGO.equals(peticion.getMetodo())) {
						respuesta.setNumReferenciaTransaccionBanco(data.substring(50, 65));//Número de Referencia de Transacción del Banco
						respuesta.setNumeroAprobacionAES(data.substring(65, 80).trim());
					}else if(OPERACION_ANULAR_PAGO.equals(peticion.getMetodo())) {
						respuesta.setNumReferenciaTransaccionBanco(data.substring(31, 46).trim());//TODO Tomado de numero transaccion
					}
					
				}
				
				exchange.getIn().setBody(respuesta);
				
				ObjectMapper mapper = new ObjectMapper();
				logger.info("RESPUESTA DEL SERVICIO ---> {}", mapper.writeValueAsString(respuesta));//DEBUG
				
			
		}

	}
	
	private static byte[] enviarTrama(Socket socket, byte[] requestBytes) throws IOException {
		DataOutputStream output = new DataOutputStream(socket.getOutputStream());
		output.write(requestBytes);
		DataInputStream input = new DataInputStream(socket.getInputStream());
		
		IsoStreamReader reader = new IsoStreamReader(socket.getInputStream());
		byte[] responseBytes = reader.readIso();
		
		output.close();
		input.close();
		
		return responseBytes;
	}
	
	private static ISOMsg crearPeticionPrincipal(String tipoSolicitud, String codTerminal, String idBanco) throws ISOException {
		ISOPackager packager = new ISO87BPackager();
		ISOMsg msg = new ISOMsg();
		msg.setPackager(packager);
		msg.set(new ISOField(0, tipoSolicitud));
		msg.set(new ISOField(3, PARAM_3));//TODO siempre tendra este valor
		msg.set(new ISOField(11, PARAM_11));//TODO siempre tendra este valor
		msg.set(new ISOField(12, ISODate.getTime(new Date())));
		msg.set(new ISOField(13, ISODate.getDate(new Date())));
		msg.set(new ISOField(41, codTerminal));
		msg.set(new ISOField(42, idBanco));
		return msg;
	}
	
	private byte[] crearPeticionConsulta(String tipoSolicitud, String codTerminal, String idBanco, String tpdu,
			ConectoresServiciosAESPeticion peticion) throws Exception {
		
		byte[] requestBytes = null;
		ISOMsg msg;
		
			msg = crearPeticionPrincipal(tipoSolicitud, codTerminal, idBanco);

			if(peticion.getNumIdentificador().length()<=8) {
				msg.set(new ISOField(48, aplFormNic(1, peticion.getNumIdentificador()) ));
			}else {
				msg.set(new ISOField(48, aplFormNic(2, peticion.getNumIdentificador()) ));
			}
			requestBytes = msg.pack();
			String requestISO = ISOUtil.hexString(requestBytes);
			requestISO = tpdu + requestISO;
			Integer longitud = requestISO.length();
			String strResp = Integer.toString(longitud/2, 16);
			strResp = ISOUtil.padleft(strResp, 4, '0');
            
			
			requestISO = strResp + requestISO;
			requestBytes = ISOUtil.hex2byte(requestISO);
			
		return requestBytes;
	}
	

	
	private byte[] crearPeticionPago(String tipoSolicitud, String codTerminal, String idBanco, String tpdu,
			ConectoresServiciosAESPeticion peticion) throws Exception{
		
		byte[] requestBytes = null;
		ISOMsg msg = null;
		Integer tipoEntrada = 0;
		if(peticion.getNumIdentificador().length()<=8) {
			tipoEntrada = 1;
		}else {
			tipoEntrada = 2;
		}
		
			msg = crearPeticionPrincipal(tipoSolicitud, codTerminal, idBanco);
			String field48 = aplFormNic(tipoEntrada,peticion.getNumIdentificador())
				+ peticion.getCodEmpresa()
				+ formatMonto(peticion.getMonto())//TODO getMontoTotal
				+ String.valueOf(peticion.getPagoAlcaldia())
				+ String.valueOf(peticion.getPagoReconexion())
				+ StringUtils.leftPad(peticion.getCodOrigen().toString(), 2,'0')
				+ formatNumeroTransaccion(peticion.getNumDocumento());//TODO registroControl.getNumeroRegistro()
			logger.info("PETICION STRING PAGO -- > "+ field48);//DEBUG
			msg.set(new ISOField(48, field48));
			requestBytes = msg.pack();
			String requestISO = ISOUtil.hexString(requestBytes);
			requestISO = tpdu + requestISO;
			Integer longitud = requestISO.length();
			
			String strResp = Integer.toString(longitud/2, 16);
			strResp = ISOUtil.padleft(strResp, 4, '0');
			requestISO = strResp + requestISO;
			
			requestBytes = ISOUtil.hex2byte(requestISO);

		return requestBytes;
	}
	
	private byte[] crearPeticionAnularPago(String tipoSolicitud, String codTerminal, String idBanco, String tpdu,
			ConectoresServiciosAESPeticion peticion) throws Exception {
		
		byte[] requestBytes = null;
		ISOMsg msg = null;
		Integer tipoEntrada = 0;
		if(peticion.getNumIdentificador().length()<=8) {
			tipoEntrada = 1;
		}else {
			tipoEntrada = 2;
		}
		
			msg = crearPeticionPrincipal(tipoSolicitud, codTerminal, idBanco);
			String field48 = aplFormNic(tipoEntrada,peticion.getNumIdentificador())
				+ formatNumeroTransaccion(peticion.getNumDocumento());
			logger.info("PETICION STRING ANULACION -- > "+ field48);//DEBUG
			msg.set(new ISOField(48, field48));
			requestBytes = msg.pack();
			String requestISO = ISOUtil.hexString(requestBytes);
			requestISO = tpdu + requestISO;
			Integer longitud = requestISO.length();
			
			String strResp = Integer.toString(longitud/2, 16);
			strResp = ISOUtil.padleft(strResp, 4, '0');
			requestISO = strResp + requestISO;
			
			
			requestBytes = ISOUtil.hex2byte(requestISO);

			
		return requestBytes;
	}
	
	public static DatosRespuesta getDatosRespuesta(byte[] responseBytes,String tpdu) throws ISOException {
		String responseISO = ISOUtil.hexString(responseBytes);
		Logger logger = LoggerFactory.getLogger("getDatosRespuesta");

		logger.info("DATOS: ---> {}", ISOUtil.hexString(responseBytes));//DEBUG
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
		
		String montoForm	= "";
		DecimalFormat f =  new DecimalFormat("##########.00");
		//String montoStr		= String.valueOf(monto);
		String montoStr		= f.format(monto);
		montoForm = StringUtils.leftPad(montoStr.replaceAll("\\.", ""), 12, '0');
		return montoForm;
	}
	
	private BigDecimal formatearBigDecimal(String monto) {
		
		StringBuilder builder = new StringBuilder();
		
		builder.append(monto.substring(0, monto.length() - 2));
		builder.append('.');
		builder.append(monto.substring(monto.length() - 2, monto.length()));

		String montoF = builder.toString();
		
		return new BigDecimal(montoF);
	}

}
