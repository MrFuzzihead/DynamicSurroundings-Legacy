package org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.implem;

import java.util.LinkedHashMap;
import java.util.Map;

import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IPrimitiveMap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BasicPrimitiveMap implements IPrimitiveMap {

    private final Map<String, String> primitiveMap = new LinkedHashMap<>();

    @Override
    public String getPrimitiveMap(String primitive) {
        return this.primitiveMap.get(primitive);
    }

    @Override
    public String getPrimitiveMapSubstrate(String primitive, String substrate) {
        return this.primitiveMap.get(primitive + "@" + substrate);
    }

    @Override
    public void register(String key, String value) {
        this.primitiveMap.put(key, value);
    }
}
