package Transactions;

import Anotations.Table;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;

@Table("Transacoes")
public class TransactionModel {
    //
    // =========== Seção de membros privados ===========
    //

    private Integer id;

    private Integer id_carteira;

    private Integer id_ativo;

    private TransactionDTO.transactionType tipo;

    private BigDecimal quantidade;

    private BigDecimal valor_unitario;

    private BigDecimal valor_total;

    private Date data_transacao;

    private BigDecimal taxa_corretagem;

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

    public TransactionDTO.transactionType getTipo() {
        return tipo;
    }

    public void setTipo(TransactionDTO.transactionType tipo) {
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

    public BigDecimal getvalor_total() {
        return valor_total;
    }

    public void setvalor_total(BigDecimal valor_total) {
        this.valor_total = valor_total;
    }

    public Date getData_transacao() {
        return data_transacao;
    }

    public void setData_transacao(Date data_transacao) {
        this.data_transacao = data_transacao;
    }

    public BigDecimal getTaxa_corretagem() {
        return taxa_corretagem;
    }

    public void setTaxa_corretagem(BigDecimal taxa_corretagem) {
        this.taxa_corretagem = taxa_corretagem;
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
