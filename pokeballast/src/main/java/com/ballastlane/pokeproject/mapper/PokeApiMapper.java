package com.ballastlane.pokeproject.mapper;

import com.ballastlane.pokeproject.bl.model.BaseStat;
import com.ballastlane.pokeproject.bl.model.BodyMeasurements;
import com.ballastlane.pokeproject.bl.model.EvolutionNode;
import com.ballastlane.pokeproject.bl.model.NamePage;
import com.ballastlane.pokeproject.bl.model.PokemonData;
import com.ballastlane.pokeproject.bl.model.SpeciesData;
import com.ballastlane.pokeproject.dto.pokeapi.PokeApiDto;
import com.ballastlane.pokeproject.util.PokeApiUrlUtil;
import com.ballastlane.pokeproject.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Anti-corruption layer: PokeAPI wire DTOs → our models, one method per PokeAPI resource.
 * PokeAPI's shape never leaks into {@code bl}.
 */
@Component
@RequiredArgsConstructor
public class PokeApiMapper {

    private static final String ENGLISH = "en";

    private final PokeApiUrlUtil urlUtil;
    private final TextNormalizer textNormalizer;

    public NamePage toNamePage(PokeApiDto.Page page) {
        return new NamePage(nullSafe(page.results()).stream().map(PokeApiDto.NamedResource::name).toList(), page.count());
    }

    public PokemonData toPokemonData(PokeApiDto.Pokemon pokemon) {
        return new PokemonData(
                pokemon.id(),
                pokemon.name(),
                pokemon.species() == null ? pokemon.name() : pokemon.species().name(),
                pokemon.sprites() == null ? null : pokemon.sprites().frontDefault(),
                officialArtwork(pokemon),
                nullSafe(pokemon.types()).stream()
                        .sorted(Comparator.comparingInt(PokeApiDto.TypeSlot::slot)).map(t -> t.type().name()).toList(),
                nullSafe(pokemon.abilities()).stream()
                        .sorted(Comparator.comparingInt(PokeApiDto.AbilitySlot::slot)).map(a -> a.ability().name()).toList(),
                new BodyMeasurements(pokemon.height(), pokemon.weight()),
                nullSafe(pokemon.stats()).stream().map(s -> new BaseStat(s.stat().name(), s.baseStat())).toList());
    }

    /** Category = English genus (e.g. "Seed Pokémon"); description = first English flavor text, cleaned. */
    public SpeciesData toSpeciesData(PokeApiDto.Species species) {
        String category = nullSafe(species.genera()).stream()
                .filter(g -> ENGLISH.equals(g.language().name()))
                .map(PokeApiDto.Genus::genus)
                .findFirst().orElse(null);
        String description = nullSafe(species.flavorTextEntries()).stream()
                .filter(f -> ENGLISH.equals(f.language().name()))
                .map(f -> textNormalizer.cleanFlavorText(f.text()))
                .findFirst().orElse(null);
        Integer chainId = species.evolutionChain() == null ? null : urlUtil.extractId(species.evolutionChain().url());
        return new SpeciesData(category, description, chainId);
    }

    public EvolutionNode toEvolutionTree(PokeApiDto.EvolutionChain chain) {
        return toEvolutionNode(chain.chain());
    }

    private EvolutionNode toEvolutionNode(PokeApiDto.ChainLink link) {
        int speciesId = urlUtil.extractId(link.species().url());
        return new EvolutionNode(speciesId, link.species().name(), urlUtil.spriteUrl(speciesId),
                nullSafe(link.evolvesTo()).stream().map(this::toEvolutionNode).toList());
    }

    private static String officialArtwork(PokeApiDto.Pokemon pokemon) {
        PokeApiDto.Sprites sprites = pokemon.sprites();
        if (sprites == null || sprites.other() == null || sprites.other().officialArtwork() == null) {
            return null;
        }
        return sprites.other().officialArtwork().frontDefault();
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
