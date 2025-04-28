package Wallets;

import Anotations.*;
import Transactions.TransactionModel;

import java.util.List;

public class WalletDTO {
    @Table("carteiras")
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

    @Table("carteiras")
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

    public static class WalletWithTransacitions extends WalletModel{

        public List<TransactionModel> transacoes;

        public WalletWithTransacitions(WalletModel walletModel) {
            this.setId(walletModel.getId());
            this.setId_usuario(walletModel.getId_usuario());
            this.setDescricao(walletModel.getDescricao());
            this.setNome(walletModel.getNome());
            this.setData_criacao(walletModel.getData_criacao());
            this.settotal_aportado(walletModel.gettotal_aportado());
            this.setRentabilidade(walletModel.getRentabilidade());
        }
    }

}