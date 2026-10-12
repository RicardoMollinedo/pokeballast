package com.ballastlane.pokeproject.repository.jpa;

import com.ballastlane.pokeproject.repository.entity.LocalPokemonEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocalPokemonJpaRepository extends JpaRepository<LocalPokemonEntity, Long> {

    /**
     * Filters are never null: an all-matching pattern ("%%") and an empty tag mean "no filter".
     * ({@code :param is null} fails on PostgreSQL with "could not determine data type of parameter".)
     *
     * @param namePattern lower-case LIKE pattern, with % and _ already escaped by the caller using '\'
     * @param tag         lower-case tag, or "" for no tag filter
     */
    @Query(value = """
            select p from LocalPokemonEntity p
            where (lower(p.name) like :namePattern escape '\\'
                   or lower(p.localizedName) like :namePattern escape '\\')
              and (:tag = '' or :tag member of p.tags)
            order by p.pokeApiId""",
            countQuery = """
            select count(p) from LocalPokemonEntity p
            where (lower(p.name) like :namePattern escape '\\'
                   or lower(p.localizedName) like :namePattern escape '\\')
              and (:tag = '' or :tag member of p.tags)""")
    Page<LocalPokemonEntity> search(@Param("namePattern") String namePattern, @Param("tag") String tag,
                                    Pageable pageable);

    @Query("select case when count(p) > 0 then true else false end from LocalPokemonEntity p where p.pokeApiId = :pokeApiId")
    boolean existsByPokeApiId(@Param("pokeApiId") int pokeApiId);
}
