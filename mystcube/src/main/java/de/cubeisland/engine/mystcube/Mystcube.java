/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cubeisland.engine.mystcube;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.recipe.Ingredient;
import de.cubeisland.engine.core.recipe.RecipeManager;
import de.cubeisland.engine.core.recipe.ShapelessIngredients;
import de.cubeisland.engine.core.recipe.condition.general.BiomeCondition;
import de.cubeisland.engine.core.recipe.condition.ingredient.DurabilityCondition;
import de.cubeisland.engine.core.recipe.condition.ingredient.MaterialCondition;
import de.cubeisland.engine.core.recipe.effect.CommandEffect;
import de.cubeisland.engine.core.recipe.effect.ExplodeEffect;
import de.cubeisland.engine.core.recipe.result.EffectResult;
import de.cubeisland.engine.core.recipe.result.item.DurabilityResult;
import de.cubeisland.engine.core.recipe.result.item.ItemStackResult;
import de.cubeisland.engine.core.recipe.result.item.KeepResult;
import de.cubeisland.engine.core.recipe.result.item.LoreResult;
import de.cubeisland.engine.core.recipe.result.item.NameResult;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.mystcube.blockpopulator.VillagePopulator;
import de.cubeisland.engine.mystcube.chunkgenerator.FlatMapGenerator;

public class Mystcube extends Module implements Listener
{
    private MystcubeConfig config;

    private RecipeManager recipeManager;

    @Override
    public void onStartupFinished()
    {
        WorldCreator worldCreator = WorldCreator.name("world_myst_flat")
                        .generator("CubeEngine:mystcube:flat")
                        .generateStructures(false)
                        .type(WorldType.FLAT)
                        .environment(Environment.NORMAL)
            ;
        World world = this.getCore().getWorldManager().createWorld(worldCreator);
        if (world != null)
        {
            world.setAmbientSpawnLimit(0);
            world.setAnimalSpawnLimit(0);
            world.setMonsterSpawnLimit(0);
            world.setSpawnFlags(false, false);

            new VillagePopulator().populate(world, new Random(), world.getSpawnLocation().getChunk());
        }

        // CUSTOM CRAFTING TEST
        ItemStack item = new ItemStack(Material.PAPER, 8);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatFormat.parseFormats("&6Magic Paper"));
        meta.setLore(Arrays.asList(ChatFormat.parseFormats("&eThe D'ni used this kind of"),
                                   ChatFormat.parseFormats("&epaper to write their Ages")));
        item.setItemMeta(meta);
        MAGIC_PAPER = item.clone();
        MAGIC_PAPER.setAmount(1);
        ShapedRecipe magicPaper = new ShapedRecipe(item).shape("ppp", "prp", "ppp").setIngredient('p', Material.PAPER).setIngredient('r', Material.REDSTONE);
        this.registerRecipe(magicPaper);

        item = new ItemStack(Material.PAPER, 1);
        meta = item.getItemMeta();
        meta.setDisplayName(ChatFormat.parseFormats("&9Raw Linking Panel"));
        meta.setLore(Arrays.asList(ChatFormat.parseFormats("&eAn unfinished linking panel."),
                                   ChatFormat.parseFormats("&eGreat heat is needed to"),
                                   ChatFormat.parseFormats("&emake it usable in a book")));
        item.setItemMeta(meta);
        RAW_PANEL = item;
        ShapelessRecipe rawLinkingPanel = new ShapelessRecipe(item);
        rawLinkingPanel.addIngredient(1, Material.PAPER).addIngredient(1, Material.DIAMOND);
        this.registerRecipe(rawLinkingPanel);

        item = new ItemStack(Material.PAPER, 1);
        meta = item.getItemMeta();
        meta.setDisplayName(ChatFormat.parseFormats("&6Linking Panel"));
        meta.setLore(Arrays.asList(ChatFormat.parseFormats("&eWhen used in an age or linking book"),
                                   ChatFormat.parseFormats("&eyou will get teleported"),
                                   ChatFormat.parseFormats("&eby merely touching the panel")));
        item.setItemMeta(meta);

        LINKING_PANEL = item;
        FurnaceRecipe linkingPanel = new FurnaceRecipe(item, Material.PAPER);
        this.registerRecipe(linkingPanel);

        item = new ItemStack(Material.INK_SACK, 1, DyeColor.GRAY.getDyeData()); // Setting Color /w MaterialData does not work WHÝ?!
        meta.setDisplayName(ChatFormat.parseFormats("&6Ash"));
        meta.setLore(Arrays.asList(ChatFormat.parseFormats("&eThis is what happens when"),
                                   ChatFormat.parseFormats("&eyou burn normal paper")));
        item.setItemMeta(meta);
        ASH = item;

        item = new ItemStack(Material.BOOK, 1);
        meta.setDisplayName(ChatFormat.parseFormats("&6Kortee'nea"));
        meta.setLore(Arrays.asList(ChatFormat.parseFormats("&eA Blank Book just"),
                                   ChatFormat.parseFormats("&ewaiting to be written")));
        item.setItemMeta(meta);
        BLANK_BOOK = item;

        this.getCore().getEventManager().registerListener(this, this);

        // TODO remove RecipeManager TEST
        this.recipeManager = new RecipeManager(this.getCore());
        this.getCore().getEventManager().registerListener(this, this.recipeManager);
        de.cubeisland.engine.core.recipe.Recipe recipe = new de.cubeisland.engine.core.recipe.Recipe(
            new ShapelessIngredients(Ingredient.withMaterial(Material.PAPER),
                                     Ingredient.withMaterial(Material.SAND).withResult(
                                         new KeepResult().withCondition(new BiomeCondition(Biome.DESERT, Biome.DESERT_HILLS))
                                                         .withChance(0.8f))),
            new ItemStackResult(Material.PAPER).and(NameResult.of("Sandpaper")).withChance(0.99f).
                            or(new ItemStackResult(Material.PAPER).and(NameResult.of("Fine Sandpaper")).
                                and(new EffectResult(new CommandEffect("broadcast A lucky Player crafted Fine SandPaper!"))).
                                and(new EffectResult(ExplodeEffect.ofSafeTnt().force(1f)))))
                .withPreview(new ItemStackResult(Material.PAPER).
                                                                    and(NameResult.of("Sandpaper")).
                                                                    and(LoreResult
                                                                            .of("1% Chance to get Fine Sandpaper", "80% Chance to keep Sand", "when crafting in Desert Biome")));
        this.recipeManager.registerRecipe(this, recipe);

        ShapelessIngredients ingredients = new ShapelessIngredients(Ingredient.withCondition(
            MaterialCondition.of(Material.WOOL).and(DurabilityCondition.exact((short)14)))); // RED WOOL
        ingredients.addIngredient(Ingredient.withCondition(MaterialCondition.of(Material.BED, Material.RED_MUSHROOM,
                Material.TNT, Material.REDSTONE, Material.REDSTONE_BLOCK, Material.REDSTONE_ORE, Material.REDSTONE_TORCH_ON,
                Material.NETHERRACK, Material.NETHER_BRICK, Material.NETHER_BRICK_ITEM, Material.NETHER_BRICK_STAIRS,
                Material.NETHER_FENCE, Material.NETHER_STALK, Material.APPLE, Material.MELON, Material.RAW_BEEF,
                Material.SPIDER_EYE, Material.FERMENTED_SPIDER_EYE, Material.RECORD_4)));
        this.recipeManager.registerRecipe(this,
            new de.cubeisland.engine.core.recipe.Recipe(ingredients,
                new ItemStackResult(Material.WOOL).and(DurabilityResult.set((short)14)).and(NameResult.of("Very Red Wool")))
            );
    }

    private Set<Recipe> myRecipes = new HashSet<>();
    private ItemStack MAGIC_PAPER;
    private ItemStack LINKING_PANEL;
    private ItemStack RAW_PANEL;
    private ItemStack ASH;
    private ItemStack BLANK_BOOK; // Kortee'nea

    // Descriptive Book: Kor-mahn
    // Linking Book: Kor'vahkh
    // Ink: lem // Use brewing if possible (water glowstone redstone inksack) (using weakness / slowness or no effect)
    // potion data 32 = thick potion

    private void registerRecipe(Recipe recipe) // TODO API in Core for Recipe Registration & unregister when unloading the module
    {
        Bukkit.getServer().addRecipe(recipe);
        this.myRecipes.add(recipe);
    }

    @Override
    public void onLoad()
    {
        this.getCore().getWorldManager().registerGenerator(this, "flat", new FlatMapGenerator());
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event)
    {
        if (event.getRecipe().getResult().getType() == Material.BOOK)
        {
            if (event.getInventory().contains(MAGIC_PAPER, 2) && event.getInventory().contains(LINKING_PANEL, 1))
            {
                event.getInventory().setResult(BLANK_BOOK.clone());
            }
        }
    }

    @EventHandler
    public void onSmelted(FurnaceSmeltEvent event)
    {
        if (event.getSource().getType() == Material.PAPER)
        {
            if (event.getSource().isSimilar(RAW_PANEL))
            {
                return;
            }
            Furnace furnace = (Furnace)event.getBlock().getState();
            int amount = furnace.getInventory().getSmelting().getAmount();
            furnace.getInventory().getSmelting().setAmount(1); // ALL Paper burned
            event.setResult(ASH.clone());
            Location furnaceLocation = furnace.getLocation();
            furnace.getWorld().playEffect(furnaceLocation.add(0, 1, 0), Effect.MOBSPAWNER_FLAMES, 4, 50);
            furnace.getWorld().playSound(furnaceLocation, Sound.FIRE, 1, 1);
            Location loc2 = new Location(null, 0,0,0);
            if (amount > 8)
            {
                for (Entity entity : furnace.getWorld().getEntities())
                {
                    if (entity instanceof Player)
                    {
                        if (entity.getLocation(loc2).distanceSquared(furnaceLocation) <= 4)
                        {
                            entity.setFireTicks(20 + amount * 2);
                        }
                    }
                }
            }
        }
    }
}
