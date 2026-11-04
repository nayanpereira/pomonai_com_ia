package br.com.senai.duvidas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DuvidaRepository extends JpaRepository<Duvida, Long> {

    /**
     * Busca todas as dúvidas ordenadas da mais recente para a mais antiga.
     * O nome "dataHora" precisa ser idêntico ao atributo da entidade Duvida.
     */
    List<Duvida> findAllByOrderByDataHoraDesc();

}