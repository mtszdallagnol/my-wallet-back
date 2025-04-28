package Wallets;


import Anotations.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Table("carteiras")
public class WalletModel {

    //
    // =========== Seção de membros privados ===========
    //

    private Integer id;

    private Integer id_usuario;

    private String nome;

    private String descricao;

    private BigDecimal total_aportado;

    private BigDecimal rentabilidade;

    private Instant data_criacao;

    private BigDecimal saldo_total;


    //
    // =========== Getters and Setters ===========
    //


    public BigDecimal getTotal_aportado() {
        return total_aportado;
    }

    public void setTotal_aportado(BigDecimal total_aportado) {
        this.total_aportado = total_aportado;
    }

    public BigDecimal getSaldo_total() {
        return saldo_total;
    }

    public void setSaldo_total(BigDecimal saldo_total) {
        this.saldo_total = saldo_total;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId_usuario() { return id_usuario; }

    public void setId_usuario(Integer id_usuario) { this.id_usuario = id_usuario; }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal gettotal_aportado() {
        return total_aportado;
    }

    public void settotal_aportado(BigDecimal total_aportado) {
        this.total_aportado = total_aportado;
    }

    public BigDecimal getRentabilidade() {
        return rentabilidade;
    }

    public void setRentabilidade(BigDecimal rentabilidade) {
        this.rentabilidade = rentabilidade;
    }

    public Instant getData_criacao() {
        return data_criacao;
    }

    public void setData_criacao(Instant data_criacao) {
        this.data_criacao = data_criacao;
    }
}
