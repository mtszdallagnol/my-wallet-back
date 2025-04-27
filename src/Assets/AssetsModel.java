package Assets;

import java.time.Instant;

public class AssetsModel {

    //                                                   //
    // =========== Seção de membros privados =========== //
    //                                                   //

    private Integer id;

    private AssetsDTO.assetType tipo;

    private String nome;

    private String simbolo;

    private String image_url;

    private Instant ultima_atualizacao;

    //                                           //
    // =========== Getters e Setters =========== //
    //                                           //

    public Integer getId() { return this.id; }

    public void setId(Integer id) { this.id = id; }

    public AssetsDTO.assetType getTipo() { return this.tipo; }

    public void setTipo(AssetsDTO.assetType tipo) { this.tipo = tipo; }

    public String getNome() { return this.nome; }

    public void setNome(String nome) { this.nome = nome; }

    public String getSimbolo() { return this.simbolo; }

    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public String getImage_url() { return this.image_url; }

    public void setImage_url(String image_url) { this.image_url = image_url; }

    public Instant getUltima_atualizacao() { return this.ultima_atualizacao; }

    public void setUltima_atualizacao(Instant ultima_atualizacao) { this.ultima_atualizacao = ultima_atualizacao; }
}
