package Wallets;


import Anotations.*;

import java.math.BigDecimal;
import java.time.Instant;

@Table(TableName = "carteiras")

public class WalletModel {

    //
    // =========== Seção de membros privados ===========
    //

    private Integer id;

    @Required
    private Integer id_usuario;

    @Required
    @MaxLength(50)
    @Unique
    private String nome;

    @MaxLength(500)
    private String descricao;

    private BigDecimal saldo_total;

    private BigDecimal rentabilidade;

    private Instant data_criacao;


    //
    // =========== Getters and Setters ===========
    //


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

    public BigDecimal getSaldo_total() {
        return saldo_total;
    }

    public void setSaldo_total(BigDecimal saldo_total) {
        this.saldo_total = saldo_total;
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
