package Goals;

import java.math.BigDecimal;
import java.sql.Date;

public class GoalModel {
    private Integer id;

    private Date data_inicial;

    private BigDecimal valor_meta;

    private BigDecimal progresso;

    private String descricao;

    private GoalDTO.GoalStatus meta_status;

    private Integer id_carteira;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getData_inicial() {
        return data_inicial;
    }

    public void setData_inicial(Date data_inicial) {
        this.data_inicial = data_inicial;
    }

    public BigDecimal getValor_meta() {
        return valor_meta;
    }

    public void setValor_meta(BigDecimal valor_meta) {
        this.valor_meta = valor_meta;
    }

    public BigDecimal getProgresso() {
        return progresso;
    }

    public void setProgresso(BigDecimal progresso) {
        this.progresso = progresso;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getId_carteira() {
        return id_carteira;
    }

    public void setId_carteira(Integer id_carteira) {
        this.id_carteira = id_carteira;
    }

    public GoalDTO.GoalStatus getMeta_status() {
        return meta_status;
    }

    public void setMeta_status(GoalDTO.GoalStatus meta_status) {
        this.meta_status = meta_status;
    }
}
