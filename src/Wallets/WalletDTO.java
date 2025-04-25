package Wallets;

import Anotations.MaxLength;
import Anotations.Required;
import Anotations.Table;
import Anotations.Unique;

public class WalletDTO {
    @Table(TableName = "carteiras")
    public static class postRequirementModel {
        @Required
        public Integer id_usuario;

        @Required
        @MaxLength(50)
        @Unique(withFields = {"id_usuario"}, message = "Carteira com mesmo nome já criada pelo usuário")
        public String nome;

        @MaxLength(200)
        public String descricao;
    }
}
