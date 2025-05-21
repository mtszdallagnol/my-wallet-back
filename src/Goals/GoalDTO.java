package Goals;

import Anotations.*;

import java.math.BigDecimal;
import java.sql.Date;

public class GoalDTO {
    public enum GoalStatus {
        ATIVA, CONCLUIDA
    }

    @Table("metas")
    public static class postRequirementModel {
        @Required
        public Integer id_usuario;

        @Required
        @Exists(withTable = "carteiras", withFields = { "id_usuario" }, message = "Carteira não existente")
        public Integer id_carteira;

        @Required
        @MaxDigits({ 15, 2 })
        @MinLength(1)
        public BigDecimal valor_meta;

        @Required
        public Date data_final;

        @MaxLength(255)
        public String descricao;
    }
}
