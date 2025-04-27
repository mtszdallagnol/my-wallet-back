package Transactions;

import Anotations.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Table("Transacoes")
public class TransactionModel {
    //
    // =========== Seção de membros privados ===========
    //

    private Integer id;

    private Integer id_carteira;

    private Integer id_ativo;

    private TransactionDTO tipo;

    private BigDecimal quantidade;

    private BigDecimal valor_unitario;

    private BigDecimal valor_total;

    private Instant data_transacao;

    private BigDecimal taxa_corretora;

    private String codigo_moeda;

    private String notas;

    private Instant data_criacao;

    private Instant data_atualizacao;

    //                                           //
    // =========== Getters e Setters =========== //
    //                                           //

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId_carteira() {
        return id_carteira;
    }

    public void setId_carteira(Integer id_carteira) {
        this.id_carteira = id_carteira;
    }

    public Integer getId_ativo() {
        return id_ativo;
    }

    public void setId_ativo(Integer id_ativo) {
        this.id_ativo = id_ativo;
    }

    public TransactionDTO getTipo() {
        return tipo;
    }

    public void setTipo(TransactionDTO tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getValor_unitario() {
        return valor_unitario;
    }

    public void setValor_unitario(BigDecimal valor_unitario) {
        this.valor_unitario = valor_unitario;
    }

    public BigDecimal getValor_total() {
        return valor_total;
    }

    public void setValor_total(BigDecimal valor_total) {
        this.valor_total = valor_total;
    }

    public Instant getData_transacao() {
        return data_transacao;
    }

    public void setData_transacao(Instant data_transacao) {
        this.data_transacao = data_transacao;
    }

    public BigDecimal getTaxa_corretora() {
        return taxa_corretora;
    }

    public void setTaxa_corretora(BigDecimal taxa_corretora) {
        this.taxa_corretora = taxa_corretora;
    }

    public String getCodigo_moeda() {
        return codigo_moeda;
    }

    public void setMoeda_codigo(String moeda_codigo) {
        this.codigo_moeda = moeda_codigo;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public Instant getData_criacao() {
        return data_criacao;
    }

    public void setData_criacao(Instant data_criacao) {
        this.data_criacao = data_criacao;
    }

    public Instant getData_atualizacao() {
        return data_atualizacao;
    }

    public void setData_atualizacao(Instant data_atulizacao) {
        this.data_atualizacao = data_atulizacao;
    }
}
