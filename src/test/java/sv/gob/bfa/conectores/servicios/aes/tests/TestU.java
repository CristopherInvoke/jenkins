package sv.gob.bfa.conectores.servicios.aes.tests;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.Socket;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.ISO87BPackager;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import sv.gob.bfa.conectores.servicios.aes.dto.ConectoresServiciosAESPeticion;
import sv.gob.bfa.conectores.servicios.aes.ex.dto.DatosRespuesta;
import sv.gob.bfa.conectores.servicios.aes.iso8583.IsoStreamReader;

public class TestU {
	
	private final static String HOST = "cato";
	
	private final static int PORT = 24000;
	
	private final static String CODIGO_TERMINAL = "0000";
	private final static String ID_BANCO = "0000602300";
	private final static String TPDU = "600001000A";
	
	private final static String TIPO_SOLICITUD_CONSULTA = "0100";
	private final static String TIPO_SOLICITUD_PAGO = "0200";
	private final static String TIPO_SOLICITUD_REVERSION = "0420";
	
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

	
	private String formatNic(Integer tipoEntrada, String valor) {
		
		String nicFormat = Integer.toString(tipoEntrada) + StringUtils.rightPad(valor, 30, '0');
		
		return nicFormat;
		
	}
	
	private static ISOMsg crearPeticionPrincipal(String tipoSolicitud, String codTerminal, String idBanco) throws ISOException {
		ISOPackager packager = new ISO87BPackager();
		ISOMsg msg = new ISOMsg();
		msg.setPackager(packager);
		msg.set(new ISOField(0, tipoSolicitud));
		msg.set(new ISOField(3, "1"));
		msg.set(new ISOField(11, "1"));
		msg.set(new ISOField(12, ISODate.getTime(new Date())));
		msg.set(new ISOField(13, ISODate.getDate(new Date())));
		msg.set(new ISOField(41, codTerminal));//TODO Verificar de donde se toma este valor
		msg.set(new ISOField(42, idBanco));//TODO Verificar de donde se toma este valor
		return msg;
	}
	
	private byte[] crearPeticionConsulta(String tipoSolicitud, String codTerminal, String idBanco, String tpdu,
			ConectoresServiciosAESPeticion peticion) {
		
		byte[] requestBytes = null;
		ISOMsg msg;
		try {
			msg = crearPeticionPrincipal(tipoSolicitud, codTerminal, idBanco);

			if(peticion.getNumIdentificador().length()==8) {
				msg.set(new ISOField(48, formatNic(1, peticion.getNumIdentificador()) ));
			}else {
				msg.set(new ISOField(48, formatNic(2, peticion.getNumIdentificador()) ));
			}
			requestBytes = msg.pack();
			String requestISO = ISOUtil.hexString(requestBytes);
			requestISO = tpdu + requestISO;
			Integer longitud = requestISO.length();
			
			String strResp = Integer.toString(longitud/2, 16);
			strResp = ISOUtil.padleft(strResp, 4, '0');
            
			
			requestISO = strResp + requestISO;
			
			requestBytes = ISOUtil.hex2byte(requestISO);
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return requestBytes;
	}
	
	public static DatosRespuesta getDatosRespuesta(byte[] responseBytes,String tpdu) throws ISOException {
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
	
	@Test
	public void prueba() throws ParseException {
		ConectoresServiciosAESPeticion peticion = new ConectoresServiciosAESPeticion();
		peticion.setMetodo("Consultar_Saldo");
		peticion.setNumIdentificador("3130111");
		
		Socket socket = null;
				try {
					
					socket = new Socket(HOST, PORT);
					byte[] requestBytes = crearPeticionConsulta(TIPO_SOLICITUD_CONSULTA, CODIGO_TERMINAL, ID_BANCO, TPDU, peticion);
					System.out.println("Peticion: "+ISOUtil.hexString(requestBytes));
					byte[] responseBytes = enviarTrama(socket, requestBytes);
					DatosRespuesta datos = getDatosRespuesta(responseBytes, TPDU);
					ObjectMapper mapper = new ObjectMapper();
					String datosString = mapper.writeValueAsString(datos);
					System.out.println("Datos: " + datosString);
//					byte[] responseBytes = recibirTrama(socket);
					socket.close();
					System.out.println("Peticion: " + requestBytes);
					System.out.println("Respuesta: " + responseBytes);
					System.out.println("Respuesta: " + ISOUtil.hexString(responseBytes));
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
		
	}
	
//	@Test
	public void pruebaFormat() {
		
		String cod = "123456";
		
		BigDecimal monto = new BigDecimal(512);
		
		System.out.println(StringUtils.leftPad(String.valueOf(monto), 12,'0'));
		System.out.println(cod.substring(0,4));
	}
	
}
