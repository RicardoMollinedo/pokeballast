package com.ballastlane.pokeproject.repository.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence model of a replicated pokemon. Separate from {@code bl.model.LocalPokemon}: JPA needs a
 * mutable class with a no-arg constructor, the business model does not.
 */
@Entity
@Table(name = "local_pokemon")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalPokemonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "poke_api_id", nullable = false, unique = true)
    private Integer pokeApiId;

    @Column(nullable = false)
    private String name;

    @Column(name = "sprite_url")
    private String spriteUrl;

    private String category;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false)
    private List<String> types = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false)
    private List<String> abilities = new ArrayList<>();

    @Column(name = "height_dm", nullable = false)
    private int heightDm;

    @Column(name = "weight_hg", nullable = false)
    private int weightHg;

    @Column(name = "localized_name")
    private String localizedName;

    private String region;

    @ElementCollection
    @CollectionTable(name = "local_pokemon_tag", joinColumns = @JoinColumn(name = "pokemon_id"))
    @OrderColumn(name = "position")
    @Column(name = "tag", nullable = false)
    @BatchSize(size = 50) // a page of pokemon loads its tags in one query instead of N
    @Setter(AccessLevel.NONE)
    private List<String> tags = new ArrayList<>();

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static LocalPokemonEntity create() {
        return new LocalPokemonEntity();
    }

    /** Mutates the collection in place so Hibernate keeps tracking the same instance. */
    public void replaceTags(List<String> newTags) {
        tags.clear();
        tags.addAll(newTags);
    }
}
