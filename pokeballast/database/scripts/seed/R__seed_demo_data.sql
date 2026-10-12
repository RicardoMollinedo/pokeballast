-- Demo data (F07). Repeatable and idempotent: Flyway re-runs it only when this file changes, and every
-- insert skips rows that already exist. Real PokeAPI values, so the app works offline on first start.
-- Not loaded by the automated tests (they set spring.flyway.locations to the schema migrations only).

-- Demo users (passwords documented in the README): ash / Pikachu123 (TRAINER), admin / Admin1234 (ADMIN).
-- BCrypt cost 12, generated with the application's own encoder. Users that already exist keep their hash.
insert into app_user (username, password_hash, role)
select 'ash', '$2a$12$CYyHWmabD5lDAFOoSQoTGebWjQEqSigyS7rnfwqSVpAhmu3.O1/Vy', 'TRAINER'
where not exists (select 1 from app_user where username = 'ash');

insert into app_user (username, password_hash, role)
select 'admin', '$2a$12$7Br8l8dWwcGjA8dqQafdOexPtuyDdbgOnzQOxfpkJqwYrAhWWeHzy', 'ADMIN'
where not exists (select 1 from app_user where username = 'admin');

-- Demo pokemon with proprietary fields filled in
insert into local_pokemon (poke_api_id, name, sprite_url, category, types, abilities, height_dm, weight_hg,
                           localized_name, region, version, synced_at, updated_at)
select 1, 'bulbasaur', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png', 'Seed Pokémon', 'grass,poison', 'overgrow,chlorophyll', 7, 69,
       'フシギダネ', 'Kanto', 0, current_timestamp, current_timestamp
where not exists (select 1 from local_pokemon where poke_api_id = 1);

insert into local_pokemon (poke_api_id, name, sprite_url, category, types, abilities, height_dm, weight_hg,
                           localized_name, region, version, synced_at, updated_at)
select 4, 'charmander', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png', 'Lizard Pokémon', 'fire', 'blaze,solar-power', 6, 85,
       'ヒトカゲ', 'Kanto', 0, current_timestamp, current_timestamp
where not exists (select 1 from local_pokemon where poke_api_id = 4);

insert into local_pokemon (poke_api_id, name, sprite_url, category, types, abilities, height_dm, weight_hg,
                           localized_name, region, version, synced_at, updated_at)
select 7, 'squirtle', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/7.png', 'Tiny Turtle Pokémon', 'water', 'torrent,rain-dish', 5, 90,
       'ゼニガメ', 'Kanto', 0, current_timestamp, current_timestamp
where not exists (select 1 from local_pokemon where poke_api_id = 7);

insert into local_pokemon (poke_api_id, name, sprite_url, category, types, abilities, height_dm, weight_hg,
                           localized_name, region, version, synced_at, updated_at)
select 25, 'pikachu', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png', 'Mouse Pokémon', 'electric', 'static,lightning-rod', 4, 60,
       'ピカチュウ', 'Kanto', 0, current_timestamp, current_timestamp
where not exists (select 1 from local_pokemon where poke_api_id = 25);

insert into local_pokemon (poke_api_id, name, sprite_url, category, types, abilities, height_dm, weight_hg,
                           localized_name, region, version, synced_at, updated_at)
select 133, 'eevee', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/133.png', 'Evolution Pokémon', 'normal', 'run-away,adaptability,anticipation', 3, 65,
       'イーブイ', 'Kanto', 0, current_timestamp, current_timestamp
where not exists (select 1 from local_pokemon where poke_api_id = 133);

insert into local_pokemon (poke_api_id, name, sprite_url, category, types, abilities, height_dm, weight_hg,
                           localized_name, region, version, synced_at, updated_at)
select 143, 'snorlax', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/143.png', 'Sleeping Pokémon', 'normal', 'immunity,thick-fat,gluttony', 21, 4600,
       null, 'Kanto', 0, current_timestamp, current_timestamp
where not exists (select 1 from local_pokemon where poke_api_id = 143);

insert into local_pokemon (poke_api_id, name, sprite_url, category, types, abilities, height_dm, weight_hg,
                           localized_name, region, version, synced_at, updated_at)
select 149, 'dragonite', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/149.png', 'Dragon Pokémon', 'dragon,flying', 'inner-focus,multiscale', 22, 2100,
       'カイリュー', 'Kanto', 0, current_timestamp, current_timestamp
where not exists (select 1 from local_pokemon where poke_api_id = 149);

insert into local_pokemon (poke_api_id, name, sprite_url, category, types, abilities, height_dm, weight_hg,
                           localized_name, region, version, synced_at, updated_at)
select 150, 'mewtwo', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/150.png', 'Genetic Pokémon', 'psychic', 'pressure,unnerve', 20, 1220,
       'ミュウツー', 'Kanto', 0, current_timestamp, current_timestamp
where not exists (select 1 from local_pokemon where poke_api_id = 150);

-- Their tags
insert into local_pokemon_tag (pokemon_id, position, tag)
select p.id, 0, 'starter' from local_pokemon p
where p.poke_api_id = 1 and not exists (select 1 from local_pokemon_tag t where t.pokemon_id = p.id and t.position = 0);

insert into local_pokemon_tag (pokemon_id, position, tag)
select p.id, 1, 'grass-team' from local_pokemon p
where p.poke_api_id = 1 and not exists (select 1 from local_pokemon_tag t where t.pokemon_id = p.id and t.position = 1);

insert into local_pokemon_tag (pokemon_id, position, tag)
select p.id, 0, 'starter' from local_pokemon p
where p.poke_api_id = 4 and not exists (select 1 from local_pokemon_tag t where t.pokemon_id = p.id and t.position = 0);

insert into local_pokemon_tag (pokemon_id, position, tag)
select p.id, 0, 'starter' from local_pokemon p
where p.poke_api_id = 7 and not exists (select 1 from local_pokemon_tag t where t.pokemon_id = p.id and t.position = 0);

insert into local_pokemon_tag (pokemon_id, position, tag)
select p.id, 0, 'mascot' from local_pokemon p
where p.poke_api_id = 25 and not exists (select 1 from local_pokemon_tag t where t.pokemon_id = p.id and t.position = 0);

insert into local_pokemon_tag (pokemon_id, position, tag)
select p.id, 1, 'electric-team' from local_pokemon p
where p.poke_api_id = 25 and not exists (select 1 from local_pokemon_tag t where t.pokemon_id = p.id and t.position = 1);

insert into local_pokemon_tag (pokemon_id, position, tag)
select p.id, 0, 'branching-evolution' from local_pokemon p
where p.poke_api_id = 133 and not exists (select 1 from local_pokemon_tag t where t.pokemon_id = p.id and t.position = 0);

insert into local_pokemon_tag (pokemon_id, position, tag)
select p.id, 0, 'tank' from local_pokemon p
where p.poke_api_id = 143 and not exists (select 1 from local_pokemon_tag t where t.pokemon_id = p.id and t.position = 0);

insert into local_pokemon_tag (pokemon_id, position, tag)
select p.id, 0, 'pseudo-legendary' from local_pokemon p
where p.poke_api_id = 149 and not exists (select 1 from local_pokemon_tag t where t.pokemon_id = p.id and t.position = 0);

insert into local_pokemon_tag (pokemon_id, position, tag)
select p.id, 0, 'legendary' from local_pokemon p
where p.poke_api_id = 150 and not exists (select 1 from local_pokemon_tag t where t.pokemon_id = p.id and t.position = 0);

insert into local_pokemon_tag (pokemon_id, position, tag)
select p.id, 1, 'restricted' from local_pokemon p
where p.poke_api_id = 150 and not exists (select 1 from local_pokemon_tag t where t.pokemon_id = p.id and t.position = 1);

