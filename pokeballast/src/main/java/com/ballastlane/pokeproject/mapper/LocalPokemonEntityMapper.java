package com.ballastlane.pokeproject.mapper;

import com.ballastlane.pokeproject.bl.model.BodyMeasurements;
import com.ballastlane.pokeproject.bl.model.Customization;
import com.ballastlane.pokeproject.bl.model.LocalPokemon;
import com.ballastlane.pokeproject.repository.entity.LocalPokemonEntity;
import org.springframework.stereotype.Component;

/** JPA entity ↔ business model. */
@Component
public class LocalPokemonEntityMapper {

    public LocalPokemon toModel(LocalPokemonEntity entity) {
        return new LocalPokemon(
                entity.getId(),
                entity.getPokeApiId(),
                entity.getName(),
                entity.getSpriteUrl(),
                entity.getCategory(),
                entity.getTypes(),
                entity.getAbilities(),
                new BodyMeasurements(entity.getHeightDm(), entity.getWeightHg()),
                new Customization(entity.getLocalizedName(), entity.getRegion(), entity.getTags()),
                entity.getVersion() == null ? 0 : entity.getVersion(),
                entity.getSyncedAt(),
                entity.getUpdatedAt());
    }

    /** Copies every persistent field except id and version, which are managed by JPA. */
    public void copyToEntity(LocalPokemon source, LocalPokemonEntity target) {
        target.setPokeApiId(source.pokeApiId());
        target.setName(source.name());
        target.setSpriteUrl(source.spriteUrl());
        target.setCategory(source.category());
        target.setTypes(source.types());
        target.setAbilities(source.abilities());
        target.setHeightDm(source.measurements().heightDecimetres());
        target.setWeightHg(source.measurements().weightHectograms());
        target.setLocalizedName(source.customization().localizedName());
        target.setRegion(source.customization().region());
        target.replaceTags(source.customization().tags());
        target.setSyncedAt(source.syncedAt());
        target.setUpdatedAt(source.updatedAt());
    }
}
