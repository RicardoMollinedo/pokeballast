package com.ballastlane.pokeproject.mapper;

import com.ballastlane.pokeproject.bl.model.EvolutionNode;
import com.ballastlane.pokeproject.bl.model.LocalPokemon;
import com.ballastlane.pokeproject.bl.model.PageResult;
import com.ballastlane.pokeproject.bl.model.PokemonDetail;
import com.ballastlane.pokeproject.bl.model.PokemonSummary;
import com.ballastlane.pokeproject.dto.LocalPokemonResponse;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.PokemonDetailResponse;
import com.ballastlane.pokeproject.dto.PokemonSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/** Business models ↔ API DTOs. */
@Component
public class PokemonResponseMapper {

    public <M, D> PageResponse<D> toPage(PageResult<M> page, Function<? super M, ? extends D> itemMapper) {
        return new PageResponse<>(page.items().stream().<D>map(itemMapper).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    public PokemonSummaryResponse toSummary(PokemonSummary summary) {
        return new PokemonSummaryResponse(summary.id(), summary.name(), summary.spriteUrl(), summary.category(),
                summary.types(), summary.abilities(),
                summary.measurements().heightMeters(), summary.measurements().weightKilograms());
    }

    public PokemonDetailResponse toDetail(PokemonDetail detail) {
        return new PokemonDetailResponse(detail.id(), detail.name(), detail.imageUrl(), detail.spriteUrl(),
                detail.category(), detail.description(), detail.types(), detail.abilities(),
                detail.measurements().heightMeters(), detail.measurements().weightKilograms(),
                detail.stats().stream().map(s -> new PokemonDetailResponse.StatResponse(s.name(), s.value())).toList(),
                detail.evolutionChain() == null ? null : toEvolution(detail.evolutionChain()));
    }

    public LocalPokemonResponse toLocal(LocalPokemon pokemon) {
        return new LocalPokemonResponse(pokemon.id(), pokemon.pokeApiId(), pokemon.name(), pokemon.spriteUrl(),
                pokemon.category(), pokemon.types(), pokemon.abilities(),
                pokemon.measurements().heightMeters(), pokemon.measurements().weightKilograms(),
                pokemon.customization().localizedName(), pokemon.customization().region(),
                pokemon.customization().tags(), pokemon.version(), pokemon.syncedAt(), pokemon.updatedAt());
    }

    private PokemonDetailResponse.EvolutionResponse toEvolution(EvolutionNode node) {
        return new PokemonDetailResponse.EvolutionResponse(node.speciesId(), node.name(), node.imageUrl(),
                node.evolvesTo().stream().map(this::toEvolution).toList());
    }
}
