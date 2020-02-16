package org.labellum.mc.dynamictreestfc;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.TemplateManager;


import com.ferreusveritas.dynamictrees.blocks.BlockBranch;
import com.ferreusveritas.dynamictrees.blocks.BlockDynamicLeaves;
import com.ferreusveritas.dynamictrees.trees.Species;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import net.dries007.tfc.api.types.Tree;
import net.dries007.tfc.api.util.ITreeGenerator;
import net.dries007.tfc.objects.blocks.BlocksTFC;
import net.dries007.tfc.objects.blocks.wood.BlockSaplingTFC;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataTFC;


public class DTFCGenerator implements ITreeGenerator
{
    private int dtRadius; //used to store useful radius between canGenerate and Generate

    public DTFCGenerator()
    {
        dtRadius = 0;
    }

    @Override
    public void generateTree(TemplateManager templateManager, World world, BlockPos blockPos, Tree tree, Random random, boolean b)
    {
        if (world.rand.nextInt(5)<2) //generate only 60% of the trees for now. Need to figure out better way
        {
            return;
        } else
        {
            //experimental plains?
            ChunkDataTFC chunkData = world.getChunk(blockPos).getCapability(ChunkDataProvider.CHUNK_DATA_CAPABILITY, null);
            if (chunkData != null)
            {
                if (((int)(chunkData.getFloraDensity() * 100)) % 10 == 0) {
                    return;
                }
            }
        }

        Species dtSpecies = ModTrees.tfcSpecies.get(tree.toString());
        SafeChunkBounds bounds = new SafeChunkBounds(world, world.getChunk(blockPos).getPos());
        dtSpecies.generate(world, blockPos.down(), world.getBiome(blockPos), random,
                dtRadius <= 0 ? dtSpecies.maxBranchRadius()/3 : dtRadius,
                bounds);
        //dtSpecies.getJoCode("JP").setCareful(true).generate(world, dtSpecies, blockPos, world.getBiome(blockPos), EnumFacing.SOUTH, 8, SafeChunkBounds.ANY);
    }

    @Override
    public boolean canGenerateTree(World world, BlockPos pos, Tree treeType)
    {
        IBlockState locState = world.getBlockState(pos);
        if (locState.getMaterial().isLiquid() || (!locState.getMaterial().isReplaceable() && !(locState.getBlock() instanceof BlockSaplingTFC)))
        {
            return false;
        }

        if (!BlocksTFC.isGrowableSoil(world.getBlockState(pos.down()))) {
            return false;
        }

        int radius = treeType.getMaxGrowthRadius(); //a safe proxy for how big our DT versions get around the trunk

        //check on ground and nearby trees
        int x;
        int y;

        SafeChunkBounds bounds = new SafeChunkBounds(world, world.getChunk(pos).getPos());
        for(x = -radius; x <= radius; ++x) {
            for(y = -radius; y <= radius; ++y) {
                if (openRadius(world, pos, x, y, bounds)) {
                    continue;
                }
                return false;
            }
        }

        // need to search for trees around us to find our max radius
        dtRadius = ModTrees.tfcSpecies.get(treeType.toString()).maxBranchRadius();
        int ht = ModTrees.tfcSpecies.get(treeType.toString()).getLowestBranchHeight();
        BlockPos dtPos = pos.add(0,ht,0); //go searching for dt blocks etc.
        for(int i = 1; i <= dtRadius; ++i) { //check cardinal directions and 45s
            if (!openRadius(world, dtPos, i, 0, bounds) || !openRadius(world, dtPos, -i, 0, bounds) ||
                !openRadius(world, dtPos, 0, i, bounds) || !openRadius(world, dtPos,0,  -i, bounds) ||
                !openRadius(world, dtPos, i/2,  i/2, bounds) || !openRadius(world, dtPos,  i/2, -i/2, bounds) ||
                !openRadius(world, dtPos, -i/2, i/2, bounds) || !openRadius(world, dtPos, -i/2, -i/2, bounds))
            {
                dtRadius = i-2;
                break;
            }
        }

        x = treeType.getMaxHeight(); //used as proxy for DT tree for now

        for(y = 1; y <= x; ++y) {
            IBlockState state = world.getBlockState(pos.up(y));
            if ((!state.getMaterial().isReplaceable() && state.getMaterial() != Material.LEAVES) ||
                 isDTBranch(state)) {
                return false;
            }
        }
        return true;
    }

    private boolean openRadius(World world, BlockPos pos, int x, int y, SafeChunkBounds bounds)
    {
        boolean origin = x == 0 && y == 0;
        return origin || !bounds.inBounds(pos.add(x,0,y), false) || //either tree origin, or it's not generated,
                isReplaceable(world, pos, x, 0, y) ||                    //or ground level block is replaceable,
                ((x > 1 || y > 1) && isReplaceable(world, pos, x, 1, y));//or block at y+1 is replaceable when >1 away from origin

    }

    private boolean isDTBranch(IBlockState state)
    {
        Block block = state.getBlock();
        return block instanceof BlockBranch || block instanceof BlockDynamicLeaves;
    }

    private boolean isReplaceable(World world, BlockPos pos, int x, int y, int z)
    {
        IBlockState state = world.getBlockState(pos.add(x, y, z));
        return state.getMaterial().isReplaceable() && !isDTBranch(state);
    }
}
    /*

Comments giving some direction

[3:16 PM]AlcatrazEscapee:I've told you before, I think the only thing you should need to make trees grow using dynamic trees is to overwrite the ITreeGenerator on init
[3:16 PM]Deyna:Yep
[3:16 PM]AlcatrazEscapee:I can make it accessable so it's not required to do private final modifications
[3:17 PM]Deyna:It might be easier to just run a check if Dynamic Trees exists and hand over the actual placement to it
[3:18 PM]Deyna:I'm no expert but I think the tree generator also handles where the trees are placed + forest generation, no?
[3:18 PM]AlcatrazEscapee:yes, and that uses TFC's temperature / rainfall restrictions to achieve natural forest distributions
[3:18 PM]AlcatrazEscapee:I don't see why you'd need to change that
[3:19 PM]AlcatrazEscapee:The ITreeGenerator is called at the point when TFC says "give me a grown tree"
[3:19 PM]Deyna:that- makes things easy
[3:19 PM]AlcatrazEscapee:You might need to special case saplings though, idk what you'd expect from that
[3:19 PM]Deyna:that makes things really easy
[3:20 PM]AlcatrazEscapee:How does dynamic trees handle saplings? Do they grow into full size trees and then grow from there?
[3:20 PM]Deyna:it places a rooty dirt block
[3:20 PM]Deyna:and then the tree grows from a sapling via growth instructions
[3:21 PM]AlcatrazEscapee:In which case, probably just registry replace TFC saplings with a new one that overrides the placement (because TFC ones will call the ITreeGenerator)
[3:21 PM]AlcatrazEscapee:Or actually, it'd probably be nicer if ITreeGenerator had a flag for if it was called by world gen or by a sapling growth
[3:22 PM]Deyna:& the rest of it is just modifying tree drop tables, registering the new items + some spritework

[8:03 AM]Dirius:Is there a way to intercept the tree generation from TFC? That would be useful for making Dynamic Tree's integration
[8:31 AM]AlcatrazEscapee:Not in the sense I think you are wanting to.
[8:31 AM]AlcatrazEscapee:However, there's probably a neat solution
[8:32 AM]AlcatrazEscapee:You'll want to replace the ITreeGenerator instance of each Tree object sometime after init
[8:32 AM]AlcatrazEscapee:Right now it's a private final field on Tree, but if that's a potential candidate for addon compatibility, it can be easily changed.
[8:33 AM]AlcatrazEscapee:If you want more control, you can registry replace the Tree objects themselves with modified ones
[8:33 AM]AlcatrazEscapee:There's no real way to inject at a higher level (i.e. stop TFC from generating any trees and only call your tree gen), but you shouldn't need to do that.

[7:28 PM]Dirius:If someone was going to go about adding dynamic trees compatibility. What would be the best way to intercept all of TFC's normal tree spawn events?
[7:29 PM]AlcatrazEscapee:Iterate through the Tree registry and call setTreeGenerator on each of the elements
[7:31 PM]AlcatrazEscapee:You will need to create a tree generator, which is basically a wrapper around an object used to create a tree, both for world gen and when a sapling grows
[7:32 PM]AlcatrazEscapee:Each different wood type has a specific one
     */

