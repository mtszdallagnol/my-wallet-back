package Transactions;

import Anotations.*;

import java.math.BigDecimal;
import java.sql.Date;

public class TransactionDTO {

    public enum transactionType {
        COMPRA, VENDA
    }

    @Table("transacoes")
    public static class postRequirementModel {
        @Required
        public Integer id_usuario;

        @Required
        @Exists(withTable = "carteiras", withFields = { "id_usuario" }, message = "Carteira não existente")
        public Integer id_carteira;

        @Required
        @Exists(withTable = "ativos")
        public String nome_ativo;

        @Required
        public transactionType tipo;

        @Required
        @MaxDigits({ 20, 8 })
        public BigDecimal quantidade;

        @Required
        @MaxDigits({ 20 , 8 })
        public BigDecimal valor_unitario;

        @Required
        public Date data_transacao;

        @MaxDigits({ 4, 3 })
        @MinLength(0)
        @MaxLength(1)
        public BigDecimal taxa_corretagem;

        @Exists(withTable = "moedas", message = "Moeda não existente ou atualmente não suportada")
        public String codigo_moeda;

        @MaxLength(200)
        public String notas;
    }
}

