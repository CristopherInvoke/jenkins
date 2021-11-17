package sv.gob.bfa.conectores.servicios.aes.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiModel;

import sv.gob.bfa.soporte.comunes.dto.Respuesta;

@XmlRootElement(name = "ConectoresServiciosAESRespuesta")
@ApiModel(description = "ConectoresServiciosAESRespuesta")
public class ConectoresServiciosAESRespuesta extends Respuesta implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Integer fechaVencimiento;
	private String nombreCliente;
	private String codEmpresa;
	private String empresa;
	private BigDecimal saldoEnergia;
	private BigDecimal saldoAlcaldia;
	private BigDecimal saldoReconexion;
	private String numReferenciaTransaccionBanco;
	private String numeroAprobacionAES;
	
	public Integer getFechaVencimiento() {
		return fechaVencimiento;
	}
	public void setFechaVencimiento(Integer fechaVencimiento) {
		this.fechaVencimiento = fechaVencimiento;
	}
	public String getNombreCliente() {
		return nombreCliente;
	}
	public void setNombreCliente(String nombreCliente) {
		this.nombreCliente = nombreCliente;
	}
	public String getCodEmpresa() {
		return codEmpresa;
	}
	public void setCodEmpresa(String codEmpresa) {
		this.codEmpresa = codEmpresa;
	}
	public String getEmpresa() {
		return empresa;
	}
	public void setEmpresa(String empresa) {
		this.empresa = empresa;
	}
	public BigDecimal getSaldoEnergia() {
		return saldoEnergia;
	}
	public void setSaldoEnergia(BigDecimal saldoEnergia) {
		this.saldoEnergia = saldoEnergia;
	}
	public BigDecimal getSaldoAlcaldia() {
		return saldoAlcaldia;
	}
	public void setSaldoAlcaldia(BigDecimal saldoAlcaldia) {
		this.saldoAlcaldia = saldoAlcaldia;
	}
	public BigDecimal getSaldoReconexion() {
		return saldoReconexion;
	}
	public void setSaldoReconexion(BigDecimal saldoReconexion) {
		this.saldoReconexion = saldoReconexion;
	}
	public String getNumeroAprobacionAES() {
		return numeroAprobacionAES;
	}
	public void setNumeroAprobacionAES(String numeroAprovacionAES) {
		this.numeroAprobacionAES = numeroAprovacionAES;
	}
	public String getNumReferenciaTransaccionBanco() {
		return numReferenciaTransaccionBanco;
	}
	public void setNumReferenciaTransaccionBanco(String numReferenciaTransaccionBanco) {
		this.numReferenciaTransaccionBanco = numReferenciaTransaccionBanco;
	}
	
}
