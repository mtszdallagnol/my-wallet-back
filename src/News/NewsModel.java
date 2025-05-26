package News;

import Anotations.Table;

import java.time.Instant;

@Table("noticias")

public class NewsModel {

        //
        // =========== Seção de membros privados ===========
        //

        private Integer id;

        private Integer analista_id;

        private String titulo;

        private String categoria;

        private String conteudo;

        private String status;

        private Instant data_criacao;

        private Instant data_atualizacao;

        //
        // =========== Getters and Setters ===========
        //

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getAnalista_id() {
            return analista_id;
        }

        public void setAnalista_id(Integer analista_id) {
            this.analista_id = analista_id;
        }

        public String getTitulo() {
            return titulo;
        }

        public void setTitulo(String titulo) {
            this.titulo = titulo;
        }

        public String getCategoria() {
            return categoria;
        }

        public void setCategoria(String categoria) {
            this.categoria = categoria;
        }

        public String getConteudo() {
            return conteudo;
        }

        public void setConteudo(String conteudo) {
            this.conteudo = conteudo;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
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

        public void setData_atualizacao(Instant data_atualizacao) {
            this.data_atualizacao = data_atualizacao;
        }
}
