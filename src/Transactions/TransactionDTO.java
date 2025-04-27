package Transactions;

import Anotations.*;

import java.math.BigDecimal;
import java.time.Instant;

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
        public Integer id_ativo;

        @Required
        public transactionType tipo;

        @Required
        @MaxDigits({20, 8})
        public BigDecimal quantidade;

        @Required
        @MaxDigits({ 20 , 8 })
        public BigDecimal valor_unitario;

        @Required
        @MaxDigits({ 20, 2 })
        public BigDecimal valor_total;

        @Required
        public Instant data_transacao;

        @MaxDigits({ 2, 2 })
        @MinLength(0)
        @MaxLength(1)
        public BigDecimal taxa_corretagem;

        @Exists(withTable = "moedas", message = "Moeda não existente ou atualmente não suportada")
        public String codigo_moeda;

        @MaxLength(200)
        public String notas;
    }
}

