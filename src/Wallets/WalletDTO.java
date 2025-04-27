package Wallets;

import Anotations.*;

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

    @Table(TableName = "carteiras")
    public static class updateRequirementModel {
        @Required
        @Exists
        public Integer id;

        @Required
        public Integer id_usuario;

        @Unique(withFields = {"id_usuario"}, message = "Carteira com mesmo nome já criada pelo usuário")
        @MaxLength(50)
        public String nome;

        @MaxLength(200)
        public String descricao;
    }
}