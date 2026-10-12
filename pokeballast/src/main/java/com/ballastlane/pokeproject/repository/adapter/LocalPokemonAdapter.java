package com.ballastlane.pokeproject.repository.adapter;

import com.ballastlane.pokeproject.bl.model.LocalPokemon;
import com.ballastlane.pokeproject.bl.model.LocalPokemonFilter;
import com.ballastlane.pokeproject.bl.model.PageQuery;
import com.ballastlane.pokeproject.bl.model.PageResult;
import com.ballastlane.pokeproject.bl.port.LocalPokemonPort;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.LocalPokemonEntityMapper;
import com.ballastlane.pokeproject.repository.entity.LocalPokemonEntity;
import com.ballastlane.pokeproject.repository.jpa.LocalPokemonJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link LocalPokemonPort} on top of Spring Data JPA. Owns the transactions and translates
 * persistence exceptions into {@link BallastException}, so JPA never leaks into bl or api.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalPokemonAdapter implements LocalPokemonPort {

    private final LocalPokemonJpaRepository jpa;
    private final LocalPokemonEntityMapper mapper;

    @Override
    public Optional<LocalPokemon> findById(long id) {
        log.info("[DB] Finding local pokemon by id - id={}", id);
        Optional<LocalPokemon> found = jpa.findById(id).map(mapper::toModel);
        log.info("[DB] Find by id returned - id={}, found={}", id, found.isPresent());
        return found;
    }

    @Override
    public boolean existsById(long id) {
        log.info("[DB] Checking local pokemon exists - id={}", id);
        boolean exists = jpa.existsById(id);
        log.info("[DB] Exists check returned - id={}, exists={}", id, exists);
        return exists;
    }

    @Override
    public boolean existsByPokeApiId(int pokeApiId) {
        log.info("[DB] Checking pokemon already imported - pokeApiId={}", pokeApiId);
        boolean exists = jpa.existsByPokeApiId(pokeApiId);
        log.info("[DB] Imported check returned - pokeApiId={}, exists={}", pokeApiId, exists);
        return exists;
    }

    @Override
    public PageResult<LocalPokemon> search(LocalPokemonFilter filter, PageQuery pageQuery) {
        String namePattern = "%" + escapeLike(normalize(filter.name())) + "%";
        String tag = normalize(filter.tag());
        log.info("[DB] Searching local pokemon - name={}, tag={}, page={}, size={}",
                filter.name(), filter.tag(), pageQuery.page(), pageQuery.size());
        Page<LocalPokemonEntity> page = jpa.search(namePattern, tag,
                PageRequest.of(pageQuery.page(), pageQuery.size()));
        log.info("[DB] Search returned - total={}, ids={}", page.getTotalElements(),
                page.getContent().stream().map(LocalPokemonEntity::getId).toList());
        return new PageResult<>(page.getContent().stream().map(mapper::toModel).toList(),
                pageQuery.page(), pageQuery.size(), page.getTotalElements());
    }

    @Override
    @Transactional
    public LocalPokemon save(LocalPokemon pokemon) {
        log.info("[DB] Saving local pokemon - id={}, pokeApiId={}, version={}",
                pokemon.id(), pokemon.pokeApiId(), pokemon.version());
        LocalPokemonEntity entity = pokemon.id() == null ? LocalPokemonEntity.create() : loadForUpdate(pokemon);
        mapper.copyToEntity(pokemon, entity);
        try {
            LocalPokemonEntity saved = jpa.saveAndFlush(entity);
            log.info("[DB] Save returned - id={}, version={}", saved.getId(), saved.getVersion());
            return mapper.toModel(saved);
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException ex) {
            // two writers raced past the business checks; the database had the last word
            log.warn("[DB] Save rejected by the database - pokeApiId={}, reason={}",
                    pokemon.pokeApiId(), ex.getClass().getSimpleName());
            throw new BallastException(BallastErrorCode.CONCURRENT_MODIFICATION);
        }
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        log.info("[DB] Deleting local pokemon - id={}", id);
        jpa.deleteById(id);
        log.info("[DB] Delete done - id={}", id);
    }

    private LocalPokemonEntity loadForUpdate(LocalPokemon pokemon) {
        LocalPokemonEntity entity = jpa.findById(pokemon.id())
                .orElseThrow(() -> new BallastException(BallastErrorCode.LOCAL_POKEMON_NOT_FOUND));
        // fast fail; @Version still protects the window between this read and the flush
        if (!Objects.equals(entity.getVersion(), pokemon.version())) {
            log.info("[DB] Stale version detected - id={}, expected={}, actual={}",
                    pokemon.id(), pokemon.version(), entity.getVersion());
            throw new BallastException(BallastErrorCode.STALE_VERSION);
        }
        return entity;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    /** User input must not be able to inject LIKE wildcards. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
