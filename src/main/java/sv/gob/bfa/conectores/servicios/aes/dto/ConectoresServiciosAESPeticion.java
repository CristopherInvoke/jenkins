package sv.gob.bfa.conectores.servicios.aes.dto;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiModel;

@XmlRootElement(name="ConectoresServiciosAESPeticion")
@ApiModel(description = "ConectoresServiciosAESPeticion")
public class ConectoresServiciosAESPeticion{

	private String metodo;
	private String numIdentificador;//Numero de npe o nic
	private Long fechaTransaccion;
	private String codEmpresa;
	private String codAgencia;
	private String codSucursal;
	private Integer pagoAlcaldia;
	private Integer pagoReconexion;
	private Integer codOrigen;
	private Long numDocumento;
	private BigDecimal monto;
	
	public String getNumIdentificador() {
		return numIdentificador;
	}
	public void setNumIdentificador(String numIdentificador) {
		this.numIdentificador = numIdentificador;
	}
	public String getMetodo() {
		return metodo;
	}
	public void setMetodo(String metodo) {
		this.metodo = metodo;
	}
	public Long getFechaTransaccion() {
		return fechaTransaccion;
	}
	public void setFechaTransaccion(Long fechaTransaccion) {
		this.fechaTransaccion = fechaTransaccion;
	}
	public String getCodEmpresa() {
		return codEmpresa;
	}
	public void setCodEmpresa(String codEmpresa) {
		this.codEmpresa = codEmpresa;
	}
	public String getCodAgencia() {
		return codAgencia;
	}
	public void setCodAgencia(String codAgencia) {
		this.codAgencia = codAgencia;
	}
	public String getCodSucursal() {
		return codSucursal;
	}
	public void setCodSucursal(String codSucursal) {
		this.codSucursal = codSucursal;
	}
	public Integer getPagoAlcaldia() {
		return pagoAlcaldia;
	}
	public void setPagoAlcaldia(Integer pagoAlcaldia) {
		this.pagoAlcaldia = pagoAlcaldia;
	}
	public Integer getPagoReconexion() {
		return pagoReconexion;
	}
	public void setPagoReconexion(Integer pagoReconexion) {
		this.pagoReconexion = pagoReconexion;
	}
	public Integer getCodOrigen() {
		return codOrigen;
	}
	public void setCodOrigen(Integer codOrigen) {
		this.codOrigen = codOrigen;
	}
	public Long getNumDocumento() {
		return numDocumento;
	}
	public void setNumDocumento(Long numDocumento) {
		this.numDocumento = numDocumento;
	}
	public BigDecimal getMonto() {
		return monto;
	}
	public void setMonto(BigDecimal monto) {
		this.monto = monto;
	}
	
}
