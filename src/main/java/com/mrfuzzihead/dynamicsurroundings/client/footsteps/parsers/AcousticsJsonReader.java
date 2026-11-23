package com.mrfuzzihead.dynamicsurroundings.client.footsteps.parsers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem.BasicAcoustic;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem.DelayedAcoustic;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem.EventSelectorAcoustics;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem.ProbabilityWeightsAcoustic;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem.SimultaneousAcoustic;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.EventType;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IAcoustic;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.ILibrary;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AcousticsJsonReader {

    private final int ENGINEVERSION = 0;
    private String soundRoot;
    private float default_volMin;
    private float default_volMax;
    private float default_pitchMin;
    private float default_pitchMax;
    private final float DIVIDE = 100.0F;

    public AcousticsJsonReader(String root) {
        this.soundRoot = root;
    }

    public void parseJSON(String jasonString, ILibrary lib) {
        try {
            this.parseJSONUnsafe(jasonString, lib);
        } catch (Exception var4) {
            ModLog.warn("Unable to parse acoustics JSON");
            ModLog.catching(var4);
        }
    }

    void parseJSONUnsafe(String jsonString, ILibrary lib) throws JsonParseException {
        JsonObject json = new JsonParser().parse(jsonString)
            .getAsJsonObject();
        if (!json.has("type") || !json.get("type")
            .isJsonPrimitive()
            || !json.get("type")
                .getAsString()
                .equals("library")) {
            throw new JsonParseException("Missing or invalid type: expected \"library\"");
        } else if (!json.has("engineversion") || !json.get("engineversion")
            .isJsonPrimitive()) {
                throw new JsonParseException("Missing engineversion");
            } else if (json.get("engineversion")
                .getAsInt() != 0) {
                    throw new JsonParseException(
                        "Unrecognised Engine version: " + 0
                            + " expected, got "
                            + json.get("engineversion")
                                .getAsInt());
                } else if (!json.has("contents") || !json.get("contents")
                    .isJsonObject()) {
                        throw new JsonParseException("Empty contents");
                    } else {
                        if (json.has("soundroot")) {
                            this.soundRoot = this.soundRoot + json.get("soundroot")
                                .getAsString();
                        }

                        this.default_volMin = 1.0F;
                        this.default_volMax = 1.0F;
                        this.default_pitchMin = 1.0F;
                        this.default_pitchMax = 1.0F;
                        if (json.has("defaults")) {
                            JsonObject defaults = json.getAsJsonObject("defaults");
                            if (defaults.has("vol_min")) {
                                this.default_volMin = this.processPitchOrVolume(defaults, "vol_min");
                            }

                            if (defaults.has("vol_max")) {
                                this.default_volMax = this.processPitchOrVolume(defaults, "vol_max");
                            }

                            if (defaults.has("pitch_min")) {
                                this.default_pitchMin = this.processPitchOrVolume(defaults, "pitch_min");
                            }

                            if (defaults.has("pitch_max")) {
                                this.default_pitchMax = this.processPitchOrVolume(defaults, "pitch_max");
                            }
                        }

                        JsonObject contents = json.getAsJsonObject("contents");

                        for (Entry<String, JsonElement> preAcoustics : contents.entrySet()) {
                            String acousticsName = preAcoustics.getKey();
                            JsonObject acousticsDefinition = preAcoustics.getValue()
                                .getAsJsonObject();
                            EventSelectorAcoustics selector = new EventSelectorAcoustics(acousticsName);
                            this.parseSelector(selector, acousticsDefinition);
                            lib.addAcoustic(selector);
                        }
                    }
    }

    private void parseSelector(EventSelectorAcoustics selector, JsonObject acousticsDefinition)
        throws JsonParseException {
        for (EventType i : EventType.values()) {
            String eventName = i.jsonName();
            if (acousticsDefinition.has(eventName)) {
                JsonElement unsolved = acousticsDefinition.get(eventName);
                IAcoustic acoustic = this.solveAcoustic(unsolved);
                selector.setAcousticPair(i, acoustic);
            }
        }
    }

    private IAcoustic solveAcoustic(JsonElement unsolved) throws JsonParseException {
        IAcoustic ret = null;
        if (unsolved.isJsonObject()) {
            ret = this.solveAcousticsCompound(unsolved.getAsJsonObject());
        } else if (unsolved.isJsonPrimitive() && unsolved.getAsJsonPrimitive()
            .isString()) {
                BasicAcoustic a = new BasicAcoustic();
                this.prepareDefaults(a);
                this.setupSoundName(a, unsolved.getAsString());
                ret = a;
            }

        if (ret == null) {
            throw new JsonParseException("Unresolved Json element: \r\n" + unsolved.toString());
        } else {
            return ret;
        }
    }

    private IAcoustic solveAcousticsCompound(JsonObject unsolved) throws JsonParseException {
        IAcoustic ret = null;
        if (unsolved.has("type") && !unsolved.get("type")
            .getAsString()
            .equals("basic")) {
            String type = unsolved.get("type")
                .getAsString();
            if (type.equals("simultaneous")) {
                List<IAcoustic> acoustics = new ArrayList<>();

                for (JsonElement subElement : unsolved.getAsJsonArray("array")) {
                    acoustics.add(this.solveAcoustic(subElement));
                }

                SimultaneousAcoustic a = new SimultaneousAcoustic(acoustics);
                ret = a;
            } else if (type.equals("delayed")) {
                DelayedAcoustic a = new DelayedAcoustic();
                this.prepareDefaults(a);
                this.setupClassics(a, unsolved);
                if (unsolved.has("delay")) {
                    a.setDelayMin(
                        unsolved.get("delay")
                            .getAsInt());
                    a.setDelayMax(
                        unsolved.get("delay")
                            .getAsInt());
                } else if (unsolved.has("delay_min") && unsolved.has("delay_max")) {
                    a.setDelayMin(
                        unsolved.get("delay_min")
                            .getAsInt());
                    a.setDelayMax(
                        unsolved.get("delay_max")
                            .getAsInt());
                } else {
                    throw new JsonParseException("Delayed acoustic needs delay or delay_min/delay_max");
                }

                ret = a;
            } else if (type.equals("probability")) {
                List<Integer> weights = new ArrayList<>();
                List<IAcoustic> acoustics = new ArrayList<>();
                JsonArray sim = unsolved.getAsJsonArray("array");
                Iterator<JsonElement> iter = sim.iterator();

                while (iter.hasNext()) {
                    JsonElement subElement = iter.next();
                    weights.add(subElement.getAsInt());
                    if (!iter.hasNext()) {
                        throw new JsonParseException("Probability has odd number of children!");
                    }

                    subElement = iter.next();
                    acoustics.add(this.solveAcoustic(subElement));
                }

                ProbabilityWeightsAcoustic a = new ProbabilityWeightsAcoustic(acoustics, weights);
                ret = a;
            }
        } else {
            BasicAcoustic a = new BasicAcoustic();
            this.prepareDefaults(a);
            this.setupClassics(a, unsolved);
            ret = a;
        }

        return ret;
    }

    private void prepareDefaults(BasicAcoustic a) {
        a.setVolMin(this.default_volMin);
        a.setVolMax(this.default_volMax);
        a.setPitchMin(this.default_pitchMin);
        a.setPitchMax(this.default_pitchMax);
    }

    private void setupSoundName(BasicAcoustic a, String soundName) throws JsonParseException {
        if (soundName == null || soundName.isEmpty()) {
            throw new JsonParseException("Sound name is empty");
        }

        if (soundName.charAt(0) != '@') {
            a.setSoundName("dynamicsurroundings:" + this.soundRoot + soundName);
        } else {
            a.setSoundName(soundName.replace("@", ""));
        }
    }

    private void setupClassics(BasicAcoustic a, JsonObject solved) throws JsonParseException {
        if (!solved.has("name")) {
            throw new JsonParseException("Basic/delayed acoustic is missing its name");
        }

        this.setupSoundName(
            a,
            solved.get("name")
                .getAsString());
        if (solved.has("vol_min")) {
            a.setVolMin(this.processPitchOrVolume(solved, "vol_min"));
        }

        if (solved.has("vol_max")) {
            a.setVolMax(this.processPitchOrVolume(solved, "vol_max"));
        }

        if (solved.has("pitch_min")) {
            a.setPitchMin(this.processPitchOrVolume(solved, "pitch_min"));
        }

        if (solved.has("pitch_max")) {
            a.setPitchMax(this.processPitchOrVolume(solved, "pitch_max"));
        }
    }

    private float processPitchOrVolume(JsonObject object, String param) {
        return object.get(param)
            .getAsFloat() / 100.0F;
    }
}
