package sv.gob.bfa.conectores.servicios.aes.iso8583;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsoStreamReader {
	
	private InputStream in;
	
	public IsoStreamReader(InputStream in) {
		this.in = in;
	}
	
	public byte[] readIso() {
		int messageLength = -2;
		int i = -1;
		try {
			
			//Obteniendo longitud de mensaje
			int byte1 = in.read();
			int byte2 = in.read();
			if (byte1 == -1 || byte2 == -1) {
				return null;
			}

			//Inicializando array
			messageLength = byte1 + byte2 ;
			byte[] iso = new byte[messageLength];
			int newByte = 0;
			
			//Leyendo mensaje
			for (i=0; i<messageLength; i++) {
				newByte = in.read();
				if (newByte == -1) {
					return null;
				}
				iso[i] = (byte)newByte;
			}

			return iso;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
